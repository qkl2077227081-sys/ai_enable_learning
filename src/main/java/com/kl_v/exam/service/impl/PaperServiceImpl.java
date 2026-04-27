package com.kl_v.exam.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kl_v.exam.entity.Paper;
import com.kl_v.exam.entity.Question;
import com.kl_v.exam.mapper.PaperMapper;
import com.kl_v.exam.mapper.QuestionMapper;
import com.kl_v.exam.service.PaperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * 试卷服务实现类
 */
@Slf4j
@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

    @Autowired
    private QuestionMapper questionMapper;


    /**
     * 根据试卷id的查询试卷详情
     * 试卷对象
     * 题目集合
     * ps：题目的选项sort正序
     * ps：所有题目根据类型排序
     *
     * @param id 试卷id
     * @return
     */
    @Override
    public Paper customPaperDetailById(Long id) {
//        单表java代码进行paper查询
        Paper paper = getById(id);
//        校验paper == null -> 抛异常
        if (paper == null) {
            throw new RuntimeException("指定id=%s的试卷已经被删除无法查看详情".formatted(id));
        }
//        根据paperid查询题目集合（中间，题目，答案，选项）
        List<Question> questionList = questionMapper.customQuertQusetionListByPaperId(id);
//        校验题目集合 == null -> 赋空集合！ log->做好记录
        if (ObjectUtils.isEmpty(questionList)) {
            paper.setQuestions(new ArrayList<Question>());
            log.warn("试卷中没有题目！可以进行试卷编辑！但是不可用于考试！！对应的试卷id：{}", id);
            return paper;
        }

        log.debug("题目信息排序前：{}",questionList);
//        对题目进行排序（选择 -> 判断 -> 简答）
        questionList.sort((o1, o2) -> Integer.compare(typeToInt(o1.getType()), typeToInt(o2.getType())));
        log.debug("题目信息排序后：{}",questionList);
//        注意：type排序，是字符类型 -》 字符 -》 对应 -》 固定的数字 1 2 3
//        进行paper题目集合赋值
        paper.setQuestions(questionList);
        return paper;
    }

    //给予类型赋值
    private int typeToInt(String type) {
        switch (type) {
            case "CHOICE":return 1;
            case "JUDGE":return 2;
            case "TEXT":return 3;
            default:return 4;
        }
    }
}