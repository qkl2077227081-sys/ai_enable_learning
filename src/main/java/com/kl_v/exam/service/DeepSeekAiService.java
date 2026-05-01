package com.kl_v.exam.service;

import com.kl_v.exam.entity.Question;
import com.kl_v.exam.vo.AiGenerateRequestVo;
import com.kl_v.exam.vo.GradingResult;
import com.kl_v.exam.vo.QuestionImportVo;

import java.util.List;

/**
 * ClassName: DeepSeekAiService
 * Package: com.kl_v.exam.service
 * Description:
 *
 * @Author V
 * @Create 2026/5/1 下午5:45
 * @Version 1.0
 */

public interface DeepSeekAiService {


    /**
     * 根据前台传递的上下文环境，生成对应的提示词
     *
     * @param request
     * @return
     */
    String buildPrompt(AiGenerateRequestVo request);

    /**
     * 封装调用kimi的模型最后返回结
     *
     * @param prompt
     * @return 返回生成题目json结果/choice/message/content
     */
    String callDeepSeekAi(String prompt) throws InterruptedException;


    /**
     * ai题目信息生成
     *
     * @param request
     * @return
     */
    List<QuestionImportVo> aiGenerateQuestions(AiGenerateRequestVo request) throws InterruptedException;


    /**
     * 使用ai进行简答题判断
     *
     * @param question
     * @param userAnswer
     * @param maxScore
     * @return
     */
    GradingResult gradingTextQuestion(Question question, String userAnswer, Integer maxScore) throws InterruptedException;

    /**
     * 生成ai考试评语
     * @param totalScore
     * @param maxScore
     * @param questionCount
     * @param correctCount
     * @return
     */
    String buildSummary(Integer totalScore, Integer maxScore, Integer questionCount, Integer correctCount) throws InterruptedException;

}
