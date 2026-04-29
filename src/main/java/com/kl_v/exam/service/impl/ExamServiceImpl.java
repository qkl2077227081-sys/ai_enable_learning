package com.kl_v.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kl_v.exam.entity.AnswerRecord;
import com.kl_v.exam.entity.ExamRecord;
import com.kl_v.exam.entity.Paper;
import com.kl_v.exam.entity.Question;
import com.kl_v.exam.mapper.ExamRecordMapper;
import com.kl_v.exam.service.AnswerRecordService;
import com.kl_v.exam.service.ExamService;
import com.kl_v.exam.service.KimiAiService;
import com.kl_v.exam.service.PaperService;
import com.kl_v.exam.vo.GradingResult;
import com.kl_v.exam.vo.StartExamVo;
import com.kl_v.exam.vo.SubmitAnswerVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 考试服务实现类
 */
@Service
@Slf4j
public class ExamServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamService {

    @Autowired
    private PaperService paperService;
    @Autowired
    private AnswerRecordService answerRecordService;
    @Autowired
    private KimiAiService kimiAiService;

    /**
     * 开始考试接口
     *
     * @param startExamVo
     * @return
     */
    @Override
    public ExamRecord startExam(StartExamVo startExamVo) {
        //创建一个考试对象并添加到数据库
        //1.校验，该学生当前是否踩在正在考试的记录！若存在返回即可
        LambdaQueryWrapper<ExamRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ExamRecord::getStudentName,startExamVo.getStudentName());
        lambdaQueryWrapper.eq(ExamRecord::getExamId,startExamVo.getPaperId());
        lambdaQueryWrapper.eq(ExamRecord::getStatus,"进行中");
        ExamRecord examRecord = getOne(lambdaQueryWrapper);
        if (examRecord != null) {
            log.debug("{}这位学生在当前试卷{}下有正在进行的考试！直接返回了",startExamVo.getStudentName(),startExamVo.getPaperId());
            return examRecord;
        }
        //不存在赋予传入的参数（学生姓名，试卷id）补全（状态，时间，切屏数量）
        examRecord = new ExamRecord();
        examRecord.setStudentName(startExamVo.getStudentName());
        examRecord.setExamId(startExamVo.getPaperId());
        examRecord.setStatus("进行中");
        examRecord.setWindowSwitches(0);
        examRecord.setStartTime(LocalDateTime.now());
        //保存即可
        save(examRecord);
        //返回即可
        return examRecord;
    }

    /**
     * 获取考试记录详情
     *
     * @param id
     * @return
     */
    @Override
    public ExamRecord customGetExamRecordById(Integer id) {
        //获取考试记录，考试记录对应的试卷对象，获取考试记录对应的答题记录集合
        //注意：答题记录和顺序和考试记录顺序相同
        //1.获取考试记录的详情
        ExamRecord examRecord = getById(id);
        if (examRecord==null) {
            throw new RuntimeException("考试记录已经被删除");
        }
        //2.获取考试记录对应试卷对象详情【试卷，题目，选项和答案】
        Paper paper = paperService.customPaperDetailById(examRecord.getExamId().longValue());
        if (paper == null) {
            throw new RuntimeException("当前考试记录的试卷被删除!开始考试失败");
        }
        //3.获取考试记录对应的答题记录集合
        LambdaQueryWrapper<AnswerRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AnswerRecord::getExamRecordId,id);
        List<AnswerRecord> answerRecords = answerRecordService.list(lambdaQueryWrapper);
        if(!ObjectUtils.isEmpty(answerRecords)){
            List<Long> questionIdList = paper.getQuestions().stream().map(Question::getId).collect(Collectors.toList());
            answerRecords.sort(new Comparator<AnswerRecord>() {
                @Override
                public int compare(AnswerRecord o1, AnswerRecord o2) {
                    int x = questionIdList.indexOf(o1.getQuestionId());
                    int y = questionIdList.indexOf(o2.getQuestionId());

                    return Integer.compare(x,y);
                }
            });

        }
        //4.数据组装即可

        examRecord.setPaper(paper);
        examRecord.setAnswerRecords(answerRecords);
        return examRecord;
    }

    /**
     * 提交考试答案
     *
     * @param examRecordId
     * @param answers
     */
    @Override
    public void customSubmitAnswer(Integer examRecordId, List<SubmitAnswerVo> answers) throws InterruptedException {
        //宏观：提交答案中间表保存 修改考试记录数据 触发开始判卷（examRecordId）
        //1.中间表保存问题
        if(!ObjectUtils.isEmpty(answers)){
            List<AnswerRecord> answerRecordList = answers.stream().map(vo -> {
                AnswerRecord answerRecord = new AnswerRecord(examRecordId, vo.getQuestionId(), vo.getUserAnswer());
                return answerRecord;
            }).collect(Collectors.toList());
            answerRecordService.saveBatch(answerRecordList);
        }
        //修改下提交的考试记录状态（状态-》已完成 || 结束时间 - 设置）
        ExamRecord examRecord = getById(examRecordId);
        examRecord.setEndTime(LocalDateTime.now());
        examRecord.setStatus("已完成");
        updateById(examRecord);

        //调用判卷的接口
        gradeExam(examRecordId);
    }

    /**
     * ai试卷批阅功能
     *
     * @param examRecordId
     * @return
     */
    @Override
    public ExamRecord gradeExam(Integer examRecordId) throws InterruptedException {
        //宏观来说获取考试记录相关信息（考试记录对象 考试记录答题记录 考试对应试卷）
        // 进行循环判卷（1.答题记录进行修改，2.总体提到总分数 总正确数）
        //修改考试记录（状态-》一批阅  修改 -》总分数）
        //进行ai评语的生成
        //修改考试记录表，返回考试记录对象

        //1.获取考试记录和相关信息（试卷和答题记录）
        ExamRecord examRecord = customGetExamRecordById(examRecordId);
        Paper paper = examRecord.getPaper();
        if (paper == null) {
            examRecord.setStatus("已批阅");
            examRecord.setAnswers("试卷丢失，无法判定成绩");
            updateById(examRecord);
            throw new RuntimeException("试卷丢失，无法判定成绩");
        }
        List<AnswerRecord> answerRecords = examRecord.getAnswerRecords();
        if(ObjectUtils.isEmpty(answerRecords)){
            //没提交
            examRecord.setStatus("已批阅");
            examRecord.setScore(0);
            examRecord.setAnswers("没有提交记录，成绩为零");
            updateById(examRecord);
            return examRecord;
        }

        //2.进行循环的判卷（1.记录总分数 2.记录正确题目数量 3.修改每个答题记录的状态值（得分，是否正确0 ，1 ，2，text -》ai评语））
        int correctNumber = 0;//正确题目类型
        int totalScore = 0;//总得分
        //报错继续 某一个记录错了，后续还需要继续判卷
        //将正确的题目转成map，方便每次判断获取正确答案
        Map<Long, Question> questionMap = paper.getQuestions().stream().collect(Collectors.toMap(Question::getId, q -> q));
        for (AnswerRecord answerRecord : answerRecords) {
            try{
                //比较正确答案
                //先获取答题记录对应的题目信息
                Question question = questionMap.get(answerRecord.getQuestionId().longValue());
                //判断题目类型（选择和判断）
                String systemAnswer = question.getAnswer().getAnswer();
                String userAnswer = answerRecord.getUserAnswer();
                if("JUDGE".equalsIgnoreCase(question.getType())){
                    userAnswer = normalizeJudgeAnswer(userAnswer);
                }
                if(!"TEXT".equals(question.getType())){
                    //选择判断
                    if (systemAnswer.equalsIgnoreCase(userAnswer)) {
                        answerRecord.setIsCorrect(1);//正确
                        answerRecord.setScore(question.getPaperScore().intValue());
                    }else {
                        answerRecord.setIsCorrect(0);//正确
                        answerRecord.setScore(0);
                    }
                }else {
                    //简答进行ai判断
                    //简答题
                    GradingResult result = kimiAiService.gradingTextQuestion(question, userAnswer, question.getPaperScore().intValue());
                    //分
                    answerRecord.setScore(result.getScore());
                    //是否正确（0 1 2）
                    if (result.getScore() == 0){
                        answerRecord.setIsCorrect(0);
                        answerRecord.setAiCorrection(result.getReason());
                    }else if(result.getScore() == question.getPaperScore().intValue()){
                        answerRecord.setIsCorrect(1);
                        answerRecord.setAiCorrection(result.getFeedback());
                    }else{
                        answerRecord.setIsCorrect(2);
                        answerRecord.setAiCorrection(result.getReason());
                    }
                    //ai评价 正确 feelback 非正确reason

                }

            }catch (Exception e){
                answerRecord.setScore(0);
                answerRecord.setIsCorrect(0);
                answerRecord.setAiCorrection("判题过程出错！");
            }
            //进行记录修改
            //进行总分数赋值
            totalScore += answerRecord.getScore();
            //进行正确题目的累加
            if(answerRecord.getIsCorrect() == 1){
                correctNumber++;
            }
            answerRecordService.updateById(answerRecord);
        }

        //进行ai生成评价，进行考试记录修改和完善

        String summary = kimiAiService.buildSummary(totalScore, paper.getTotalScore().intValue(), paper.getQuestionCount(), correctNumber);

        examRecord.setScore(totalScore);
        examRecord.setAnswers(summary);
        examRecord.setStatus("已批阅");
        updateById(examRecord);
        return examRecord;
    }


    /**
     * 标准化判断题答案，将T/F转换为TRUE/FALSE
     * @param answer 原始答案
     * @return 标准化后的答案
     */
    private String normalizeJudgeAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return "";
        }

        String normalized = answer.trim().toUpperCase();
        switch (normalized) {
            case "T":
            case "TRUE":
            case "正确":
                return "TRUE";
            case "F":
            case "FALSE":
            case "错":
                return "FALSE";
            default:
                return normalized;
        }
    }
}