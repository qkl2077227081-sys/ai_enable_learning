package com.kl_v.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kl_v.exam.entity.Paper;
import com.kl_v.exam.vo.AiPaperVo;
import com.kl_v.exam.vo.PaperVo;

/**
 * 试卷服务接口
 */
public interface PaperService extends IService<Paper> {

    /**
     * 根据试卷id的查询试卷详情
     * 试卷对象
     * 题目集合
     * ps：题目的选项sort正序
     * ps：所有题目根据类型排序
     * @param id 试卷id
     * @return
     */
    Paper customPaperDetailById(Long id);


    /**
     * 手动组卷
     * @param paperVo
     * @return
     */
    Paper customCreatePaper(PaperVo paperVo);

    /**
     * ai智能组卷
     * @param aiPaperVo
     * @return
     */

    Paper customAiCreatePaper(AiPaperVo aiPaperVo);
}