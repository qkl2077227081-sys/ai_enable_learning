package com.kl_v.exam.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.kl_v.exam.config.properties.DeepSeekProperties;
import com.kl_v.exam.entity.Question;
import com.kl_v.exam.service.DeepSeekAiService;
import com.kl_v.exam.vo.AiGenerateRequestVo;
import com.kl_v.exam.vo.GradingResult;
import com.kl_v.exam.vo.QuestionImportVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

/**
 * ClassName: DeepSeekAiGradingServiceImpl
 * Package: com.kl_v.exam.service.impl
 * Description:
 *
 * @Author V
 * @Create 2026/5/1 下午5:46
 * @Version 1.0
 */
@Slf4j
@Service
public class DeepSeekAiGradingServiceImpl implements DeepSeekAiService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private DeepSeekProperties deepSeekProperties;

    /**
     * 根据前台传递的上下文环境，生成对应的提示词
     * @param request
     * @return
     */
    @Override
    public String buildPrompt(AiGenerateRequestVo request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请为我生成").append(request.getCount()).append("道关于【")
                .append(request.getTopic()).append("】的题目。\n\n");

        prompt.append("要求：\n");

        // 题目类型要求
        if (request.getTypes() != null && !request.getTypes().isEmpty()) {
            List<String> typeList = Arrays.asList(request.getTypes().split(","));
            prompt.append("- 题目类型：");
            for (String type : typeList) {
                switch (type.trim()) {
                    case "CHOICE":
                        prompt.append("选择题(**重要，最多四个选项，不可生成其他题型)");
                        if (request.getIncludeMultiple() != null && request.getIncludeMultiple()) {
                            prompt.append("(包含单选和多选)");
                        }
                        prompt.append(" ");
                        break;
                    case "JUDGE":
                        prompt.append("判断题（**重要：确保正确答案和错误答案的数量大致平衡，不要全部都是正确或错误且不可生成其他题型**） ");
                        break;
                    case "TEXT":
                        prompt.append("简答题");
                        break;
                }
            }
            prompt.append("\n");
        }

        // 难度要求
        if (request.getDifficulty() != null) {
            String difficultyText = switch (request.getDifficulty()) {
                case "EASY" -> "简单";
                case "MEDIUM" -> "中等";
                case "HARD" -> "困难";
                default -> "中等";
            };
            prompt.append("- 难度等级：").append(difficultyText).append("\n");
        }

        // 额外要求
        if (request.getRequirements() != null && !request.getRequirements().isEmpty()) {
            prompt.append("- 特殊要求：").append(request.getRequirements()).append("\n");
        }

        // 判断题特别要求
        if (request.getTypes() != null && request.getTypes().contains("JUDGE")) {
            prompt.append("- **判断题特别要求**：\n");
            prompt.append("  * 确保生成的判断题中，正确答案(TRUE)和错误答案(FALSE)的数量尽量平衡\n");
            prompt.append("  * 不要所有判断题都是正确的或都是错误的\n");
            prompt.append("  * 错误的陈述应该是常见的误解或容易混淆的概念\n");
            prompt.append("  * 正确的陈述应该是重要的基础知识点\n");
        }

        prompt.append("\n请严格按照以下JSON格式返回，不要包含任何其他文字：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"questions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"title\": \"题目内容\",\n");
        prompt.append("      \"type\": \"CHOICE|JUDGE|TEXT\",\n");
        prompt.append("      \"multi\": true/false,\n");
        prompt.append("      \"difficulty\": \"EASY|MEDIUM|HARD\",\n");
        prompt.append("      \"score\": 5,\n");
        prompt.append("      \"choices\": [\n");
        prompt.append("        {\"content\": \"选项内容\", \"isCorrect\": true/false, \"sort\": 1}\n");
        prompt.append("      ],\n");
        prompt.append("      \"answer\": \"TRUE或FALSE(判断题专用)|文本答案(简答题专用)\",\n");
        prompt.append("      \"analysis\": \"题目解析\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("注意：\n");
        prompt.append("1. 选择题必须有choices数组，判断题和简答题设置answer字段\n");
        prompt.append("2. 多选题的multi字段设为true，单选题设为false\n");
        prompt.append("3. **判断题的answer字段只能是\"TRUE\"或\"FALSE\"，请确保答案分布合理**\n");
        prompt.append("4. 每道题都要有详细的解析\n");
        prompt.append("5. 题目要有实际价值，贴近实际应用场景\n");
        prompt.append("6. 严格按照JSON格式返回，确保可以正确解析\n");

        // 如果只生成判断题，额外强调答案平衡
        if (request.getTypes() != null && request.getTypes().equals("JUDGE") && request.getCount() > 1) {
            prompt.append("7. **判断题答案分布要求**：在").append(request.getCount()).append("道判断题中，");
            int halfCount = request.getCount() / 2;
            if (request.getCount() % 2 == 0) {
                prompt.append("请生成").append(halfCount).append("道正确(TRUE)和").append(halfCount).append("道错误(FALSE)的题目");
            } else {
                prompt.append("请生成约").append(halfCount).append("-").append(halfCount + 1).append("道正确(TRUE)和约").append(halfCount).append("-").append(halfCount + 1).append("道错误(FALSE)的题目");
            }
        }

        return prompt.toString();
    }


    /**
     *
     * 进行失败重试 给3次机会！！！
     *    kimi失败场景
     *      假失败 -》 调用成功 1. 结果格式不对  2. 速率限制 -> try
     *        |
     *      真失败 -》 抛出异常 -> catch
     * 封装调用kimi模型，最终返结果
     * @param prompt
     * @return 返回生成题目json 结果 / choices / message / content
     */
    /**
     * 调用 DeepSeek AI 接口
     * 逻辑：手动循环尝试连接，区分业务异常与网络异常
     */
    public String callDeepSeekAi(String prompt) {
        // 1. 准备请求参数
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", deepSeekProperties.getModel());
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        requestBody.put("messages", messages);
        requestBody.put("temperature", deepSeekProperties.getTemperature());
        requestBody.put("max_tokens", deepSeekProperties.getMaxTokens());

        int maxAttempts = 3; // 最大尝试次数
        Exception lastException = null;

        // 2. 手动循环尝试“连接并调用”
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                log.info("开始第 {} 次尝试连接 DeepSeek API...", i);

                // 发起单次请求
                String responseJson = webClient.post()
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(60)) // 单次请求超时控制
                        .block(); // 同步阻塞获取

                // 3. 校验并解析结果
                return parseAndValidateResponse(responseJson);

            } catch (WebClientResponseException e) {
                lastException = e;
                // 关键：判断是否需要重试
                // 401(Unauthorized), 403(Forbidden), 400(Bad Request) 不需要重试
                if (e.getStatusCode().is4xxClientError()) {
                    log.error("DeepSeek 客户端请求错误 ({})，停止重试。错误原因: {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("AI 服务鉴权或参数异常，请检查配置", e);
                }

                // 5xx 错误或其它错误，记录日志并进入循环重试
                log.warn("第 {} 次调用 DeepSeek 失败，服务器返回: {}", i, e.getStatusCode());
            } catch (Exception e) {
                lastException = e;
                log.warn("第 {} 次尝试连接 DeepSeek 发生异常: {}", i, e.getMessage());
            }

            // 如果不是最后一次尝试，则等待一会再重试
            if (i < maxAttempts) {
                try {
                    Thread.sleep(1000 * i); // 递增等待时间
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // 4. 重试耗尽后的处理
        throw new RuntimeException("已尝试 " + maxAttempts + " 次连接 DeepSeek AI 均失败，最后一次错误: "
                + (lastException != null ? lastException.getMessage() : "未知"));
    }

    /**
     * 内部方法：解析 JSON 响应
     */
    private String parseAndValidateResponse(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            throw new RuntimeException("DeepSeek 返回了空响应内容");
        }

        JSONObject jsonObject = JSONObject.parseObject(responseJson);

        // 检查 API 内部错误码
        if (jsonObject.containsKey("error")) {
            String errorMsg = jsonObject.getJSONObject("error").getString("message");
            throw new RuntimeException("DeepSeek API 业务错误: " + errorMsg);
        }

        JSONArray choices = jsonObject.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("AI 响应 choices 节点为空");
        }

        String content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        if (content == null || content.isBlank()) {
            throw new RuntimeException("AI 生成的内容为空");
        }

        return content;
    }



    /**
     * ai题目信息生成
     * @param request
     * @return
     */
    @Override
    public List<QuestionImportVo> aiGenerateQuestions(AiGenerateRequestVo request) throws InterruptedException {
        //1. 校验工作
        //2. 调用方法生成提示词
        String prompt = buildPrompt(request);
        //3. 调用kimi调用方法获取结果
        String content = callDeepSeekAi(prompt);
        //4. 结果内容解析
        /*
           ```json
              {
                 questions:[{},{},{}]
              }
           ```
         */
        int startIndex = content.indexOf("```json");
        int endIndex = content.lastIndexOf("```");
        //保证有数据，且下标正确！
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            //获取真正结果
            String realResult = content.substring(startIndex+7,endIndex);
            System.out.println("realResult = " + realResult);
            JSONObject jsonObject = JSONObject.parseObject(realResult);
            JSONArray questions = jsonObject.getJSONArray("questions");
            List<QuestionImportVo> questionImportVoList = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                //获取对象
                JSONObject questionJson = questions.getJSONObject(i);
                QuestionImportVo questionImportVo = new QuestionImportVo();
                questionImportVo.setTitle(questionJson.getString("title"));
                questionImportVo.setType(questionJson.getString("type"));
                questionImportVo.setMulti(questionJson.getBoolean("multi"));
                questionImportVo.setDifficulty(questionJson.getString("difficulty"));
                questionImportVo.setScore(questionJson.getInteger("score"));
                questionImportVo.setAnalysis(questionJson.getString("analysis"));
                questionImportVo.setCategoryId(request.getCategoryId());

                //选择题处理选项
                if ("CHOICE".equals(questionImportVo.getType())) {
                    JSONArray choices = questionJson.getJSONArray("choices");
                    List<QuestionImportVo.ChoiceImportDto> choiceImportDtoList = new ArrayList<>(choices.size());
                    for (int i1 = 0; i1 < choices.size(); i1++) {
                        JSONObject choicesJSONObject = choices.getJSONObject(i1);
                        QuestionImportVo.ChoiceImportDto choiceImportDto = new QuestionImportVo.ChoiceImportDto();
                        choiceImportDto.setContent(choicesJSONObject.getString("content"));
                        choiceImportDto.setIsCorrect(choicesJSONObject.getBoolean("isCorrect"));
                        choiceImportDto.setSort(choicesJSONObject.getInteger("sort"));
                        choiceImportDtoList.add(choiceImportDto);
                    }
                    questionImportVo.setChoices(choiceImportDtoList);
                }
                //答案 [判断题！ TRUE |FALSE  false true  f  t 是 否]
                questionImportVo.setAnswer(questionJson.getString("answer"));
                questionImportVoList.add(questionImportVo);
            }
            return questionImportVoList;
        }
        throw new RuntimeException("ai生成题目json数据结构错误，无法正常解析！数据为：%s".formatted(content));
    }

    /**
     * 使用ai进行简答题判断
     *
     * @param question
     * @param userAnswer
     * @param maxScore
     * @return
     */
    @Override
    public GradingResult gradingTextQuestion(Question question, String userAnswer, Integer maxScore) throws InterruptedException {
        //生成ai调用的提示词
        String gradingPrompt = buildGradingPrompt(question, userAnswer, maxScore);
        //调用ai模型获取返回结果
        String content = callDeepSeekAi(gradingPrompt);
        //进行结果的解析
//        prompt.append("{\n");
//        prompt.append("  \"score\": 实际得分(整数),\n");
//        prompt.append("  \"feedback\": \"具体的评价反馈(50字以内)\",\n");
//        prompt.append("  \"reason\": \"扣分原因或得分依据(30字以内)\"\n");
//        prompt.append("}");
        com.alibaba.fastjson2.JSONObject jsonObject = JSON.parseObject(content);
        Integer aiScore = jsonObject.getInteger("score");
        String feedback = jsonObject.getString("feedback");
        String reason = jsonObject.getString("reason");

        if (aiScore>maxScore)aiScore = maxScore;
        if (aiScore<0)aiScore = 0;

        GradingResult gradingResult = new GradingResult(aiScore, feedback, reason);


        return gradingResult;
    }

    /**
     * 生成ai考试评语
     *
     * @param totalScore
     * @param maxScore
     * @param questionCount
     * @param correctCount
     * @return
     */
    @Override
    public String buildSummary(Integer totalScore, Integer maxScore, Integer questionCount, Integer correctCount) throws InterruptedException {
        //构建提示词
        String summaryPrompt = buildSummaryPrompt(totalScore, maxScore, questionCount, correctCount);
        //调用kimiai
        String content = callDeepSeekAi(summaryPrompt);
        //结果解析
        return content;
    }

    /**
     * 构建判卷提示词
     */
    private String buildGradingPrompt(Question question, String userAnswer, Integer maxScore) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名专业的考试阅卷老师，请对以下题目进行判卷：\n\n");

        prompt.append("【题目信息】\n");
        prompt.append("题型：").append(getQuestionTypeText(question.getType())).append("\n");
        prompt.append("题目：").append(question.getTitle()).append("\n");
        prompt.append("标准答案：").append(question.getAnswer().getAnswer()).append("\n");
        prompt.append("满分：").append(maxScore).append("分\n\n");

        prompt.append("【学生答案】\n");
        prompt.append(userAnswer.trim().isEmpty() ? "（未作答）" : userAnswer).append("\n\n");

        prompt.append("【判卷要求】\n");
        if ("CHOICE".equals(question.getType()) || "JUDGE".equals(question.getType())) {
            prompt.append("- 客观题：答案完全正确得满分，答案错误得0分\n");
        } else if ("TEXT".equals(question.getType())) {
            prompt.append("- 主观题：根据答案的准确性、完整性、逻辑性进行评分\n");
            prompt.append("- 答案要点正确且完整：80-100%分数\n");
            prompt.append("- 答案基本正确但不够完整：60-80%分数\n");
            prompt.append("- 答案部分正确：30-60%分数\n");
            prompt.append("- 答案完全错误或未作答：0分\n");
        }

        prompt.append("\n请按以下JSON格式返回判卷结果：\n");
        prompt.append("{\n");
        prompt.append("  \"score\": 实际得分(整数),\n");
        prompt.append("  \"feedback\": \"具体的评价反馈(50字以内)\",\n");
        prompt.append("  \"reason\": \"扣分原因或得分依据(30字以内)\"\n");
        prompt.append("}");

        return prompt.toString();
    }
    /**
     * 获取题目类型文本
     */
    private String getQuestionTypeText(String type) {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("CHOICE", "选择题");
        typeMap.put("JUDGE", "判断题");
        typeMap.put("TEXT", "简答题");
        return typeMap.getOrDefault(type, "未知题型");
    }


    /**
     * 构建考试总评提示词
     */
    private String buildSummaryPrompt(Integer totalScore, Integer maxScore, Integer questionCount, Integer correctCount) {
        double percentage = (double) totalScore / maxScore * 100;

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名资深的IT行业教育专家，请为学生的考试表现提供专业的总评和学习建议：\n\n");

        prompt.append("【考试成绩】\n");
        prompt.append("总得分：").append(totalScore).append("/").append(maxScore).append("分\n");
        prompt.append("得分率：").append(String.format("%.1f", percentage)).append("%\n");
        prompt.append("题目总数：").append(questionCount).append("道\n");
        prompt.append("答对题数：").append(correctCount).append("道\n\n");

        prompt.append("【要求】\n");
        prompt.append("请提供一份150字左右的考试总评，包括：\n");
        prompt.append("1. 对本次考试表现的客观评价\n");
        prompt.append("2. 指出优势和不足之处\n");
        prompt.append("3. 提供具体的学习建议和改进方向\n");
        prompt.append("4. 给予鼓励和激励\n\n");

        prompt.append("请直接返回总评内容，无需特殊格式：");

        return prompt.toString();
    }
}