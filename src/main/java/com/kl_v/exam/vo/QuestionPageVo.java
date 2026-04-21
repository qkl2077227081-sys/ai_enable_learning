package com.kl_v.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * ClassName: QuestionPageVo
 * Package: com.kl_v.exam.vo
 * Description:
 *
 * @Author V
 * @Create 2026/4/21 下午3:38
 * @Version 1.0
 */
@Data
@Schema(description = "接受四个分页参数的Vo")
public class QuestionPageVo {
    private Long categoryId;
    private String difficulty;
    private String type;
    private String keyword;
}
