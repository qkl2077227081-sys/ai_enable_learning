package com.kl_v.exam.service;


import com.kl_v.exam.vo.AiGenerateRequestVo;
import com.kl_v.exam.vo.QuestionImportVo;

import java.util.List;

/**
 * Kimi AI服务接口
 * 用于调用Kimi API生成题目
 */
public interface KimiAiService {

    /**
     * 根据前台传递的上下文环境，生成对应的提示词
     * @param request
     * @return
     */
    String buildPrompt(AiGenerateRequestVo request);

    /**
     * 封装调用kimi的模型最后返回结
     * @param prompt
     * @return 返回生成题目json结果/choice/message/content
     */
    String callKimiAi(String prompt) throws InterruptedException;


    /**
     * ai题目信息生成
     * @param request
     * @return
     */
    List<QuestionImportVo> aiGenerateQuestions(AiGenerateRequestVo request) throws InterruptedException;
}