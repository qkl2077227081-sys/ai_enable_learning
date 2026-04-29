package com.kl_v.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kl_v.exam.entity.ExamRecord;
import com.kl_v.exam.vo.StartExamVo;
import com.kl_v.exam.vo.SubmitAnswerVo;

import java.util.List;

/**
 * 考试服务接口
 */
public interface ExamService extends IService<ExamRecord> {


    /**
     * 开始考试接口
     * @param startExamVo
     * @return
     */
    ExamRecord startExam(StartExamVo startExamVo);

    /**
     * 获取考试记录详情
     * @param id
     * @return
     */
    ExamRecord customGetExamRecordById(Integer id);

    /**
     * 提交考试答案
     * @param examRecordId
     * @param answers
     */
    void customSubmitAnswer(Integer examRecordId, List<SubmitAnswerVo> answers);

    /**
     * ai试卷批阅功能
     * @param examRecordId
     * @return
     */
    ExamRecord gradeExam(Integer examRecordId);
}
 