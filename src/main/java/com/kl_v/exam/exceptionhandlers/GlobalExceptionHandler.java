package com.kl_v.exam.exceptionhandlers;

import com.kl_v.exam.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ClassName: GlobalExceptionHandler
 * Package: com.kl_v.exam.exceptionhandlers
 * Description:
 *
 * @Author V
 * @Create 2026/4/19 下午3:15
 * @Version 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public Result exception(Exception e){
        e.printStackTrace();
        //记录日志
        log.error("服务器发生运行时异常！异常信息为：{}",e.getMessage());
        //返回对应提示
        return Result.error(e.getMessage());
    }
}
