package com.kl_v.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kl_v.exam.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 公告Mapper接口
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {

    /**
     * 获取启用的公告，按优先级降序，创建时间降序排列
     * @return 公告列表
     */
    List<Notice> selectActiveNotices();

    /**
     * 获取最新的几条公告
     * @param limit 限制数量
     * @return 公告列表
     */
    List<Notice> selectLatestNotices(int limit);
    
} 