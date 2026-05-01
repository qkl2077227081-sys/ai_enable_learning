package com.kl_v.exam.controller;


import com.kl_v.exam.common.Result;
import com.kl_v.exam.service.DeepSeekAiService;
import com.kl_v.exam.service.QuestionService;
import com.kl_v.exam.utils.ExcelUtil;
import com.kl_v.exam.vo.AiGenerateRequestVo;
import com.kl_v.exam.vo.QuestionImportVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 题目批量管理控制器 - 处理题目批量操作相关的HTTP请求
 * 包括Excel导入、AI生成题目、批量验证等功能
 */
@Slf4j  // 日志注解
@RestController  // REST控制器，返回JSON数据
@RequestMapping("/api/questions/batch")  // 题目批量操作API路径前缀
@CrossOrigin(origins = "*")  // 允许跨域访问
@Tag(name = "题目批量操作", description = "题目批量管理相关操作，包括Excel导入、AI生成题目、批量验证等功能")  // Swagger API分组
public class QuestionBatchController {
    @Autowired
    private QuestionService questionService;

    

    /**
     * 下载Excel导入模板
     * @return Excel模板文件
     */
    @GetMapping("/template")  // 处理GET请求
    @Operation(summary = "下载Excel导入模板", description = "下载题目批量导入的Excel模板文件")  // API描述
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        //1.获取下载模板的字节数组
        byte[] template = ExcelUtil.generateTemplate();

        //2.封装ResponseEntity
        ResponseEntity<byte[]> responseEntity = ResponseEntity.ok()
                .header("Content-Disposition","attachment;filename=question_import_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(template);//二进制文件，不确定类型

      return responseEntity;
    }
    
    /**
     * 预览Excel文件内容（不入库）
     * @param file Excel文件
     * @return 解析出的题目列表
     */
    @PostMapping("/preview-excel")  // 处理POST请求
    @Operation(summary = "预览Excel文件内容", description = "解析并预览Excel文件中的题目内容，不会导入到数据库")  // API描述
    public Result<List<QuestionImportVo>> previewExcel(
            @Parameter(description = "Excel文件，支持.xls和.xlsx格式") @RequestParam("file") MultipartFile file) throws IOException {
//        1. 参数校验 文件不能为null || 文件是xls或者xlsx
        List<QuestionImportVo> questionImportVoList = questionService.preVirwExcel(file);
        log.info("预览解析execl接口调用成功！题目数量：{}，数据为：{}",questionImportVoList.size(),questionImportVoList);
//        2. 调用解析方法
//        3. 返回结果
       return Result.success(questionImportVoList);
    }
    
    /**
     * 从Excel文件批量导入题目
     * @param file Excel文件
     * @return 导入结果
     */
    @PostMapping("/import-excel")  // 处理POST请求
    @Operation(summary = "从Excel文件批量导入题目", description = "解析Excel文件并将题目批量导入到数据库")  // API描述
    public Result<String> importFromExcel(
            @Parameter(description = "Excel文件，包含题目数据") @RequestParam("file") MultipartFile file) throws IOException {
        String result = questionService.importExeclBatchQuestions(file);
        log.info(result);
        return Result.success(result);
    }



    @Autowired
    private DeepSeekAiService deepSeekAiService;
    /**
     * 使用AI生成题目（预览，不入库）
     * @param request AI生成请求参数
     * @return 生成的题目列表
     */
    @PostMapping("/ai-generate")  // 处理POST请求
    @Operation(summary = "AI智能生成题目", description = "使用AI技术根据指定主题和要求智能生成题目，支持预览后再决定是否导入")  // API描述
    public Result<List<QuestionImportVo>> generateQuestionsByAi(
            @RequestBody @Validated AiGenerateRequestVo request) throws InterruptedException {
        List<QuestionImportVo> questionImportVoList = deepSeekAiService.aiGenerateQuestions(request);
        log.info("使用ai生成：{} 为标题的题目成功！ 计划生成：{}道题，实际生成：{}道题！",
                request.getTopic(),request.getCount(),questionImportVoList.size());
        return Result.success(questionImportVoList);
    }

    /**
     * 批量导入题目（通用接口，支持Excel导入或AI生成后的确认导入）
     * @param questions 题目导入DTO列表
     * @return 导入结果
     */
    @PostMapping("/import-questions")  // 处理POST请求
    @Operation(summary = "批量导入题目", description = "将题目列表批量导入到数据库，支持Excel解析后的导入或AI生成后的确认导入")  // API描述
    public Result<String> importQuestions(@RequestBody List<QuestionImportVo> questions) {
        int successCount =  questionService.importBatchQuestions(questions);
        log.info("批量导入题目接口调用成功！ 一共：{}题目需要导入，成功导入了：{}道题！" ,questions.size(),successCount);
        return Result.success("批量导入题目接口调用成功！ 一共：%s 题目需要导入，成功导入了：%s 道题！".formatted(questions.size(),successCount));

    }

} 