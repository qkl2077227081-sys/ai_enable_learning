package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kl_v.exam.entity.VideoView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 视频观看记录Mapper接口
 * 提供视频观看统计相关的数据访问操作
 */
@Mapper
public interface VideoViewMapper extends BaseMapper<VideoView> {
    
    /**
     * 获取视频的观看总数
     * @param videoId 视频ID
     * @return 观看总数
     */
    Long getViewCountByVideoId(@Param("videoId") Long videoId);
    
    /**
     * 获取视频的平均观看时长
     * @param videoId 视频ID
     * @return 平均观看时长（秒）
     */
    Double getAverageViewDuration(@Param("videoId") Long videoId);
    
    /**
     * 获取观看统计数据（按日期分组）
     * @param videoId 视频ID
     * @param days 统计天数
     * @return 观看统计列表
     */
    List<Map<String, Object>> getViewStatsByDate(@Param("videoId") Long videoId, @Param("days") Integer days);
} 