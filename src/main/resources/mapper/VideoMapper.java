package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kl_v.exam.entity.Video;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 视频信息Mapper接口
 * 提供视频相关的复杂数据访问操作
 */
@Mapper
public interface VideoMapper extends BaseMapper<Video> {
    
    /**
     * 分页查询已发布的视频列表（包含分类名称）
     * @param page 分页对象
     * @param categoryId 分类ID（可选）
     * @param keyword 搜索关键字（可选）
     * @return 视频分页结果
     */
    IPage<Video> getPublishedVideosPage(Page<Video> page,
                                        @Param("keyword") String keyword,
                                        @Param("categoryId") Long categoryId,
                                        @Param("status") Integer status,
                                        @Param("uploaderType") Integer uploaderType
    );

    /**
     * 如果你想保留原来的调用习惯，不修改 Service 层代码，可以新增一个默认实现（Java 8+）
     * 这样原有的 videoMapper.getPublishedVideosPage(page, keyword, catId) 依然有效
     */
    default IPage<Video> getPublishedVideosPage(Page<Video> page, String keyword, Long categoryId) {
        return getPublishedVideosPage(page, keyword, categoryId, null, null);
    }// 即使XML没用到，既然接口有也留着
    
    /**
     * 管理端分页查询视频列表（包含分类名称和审核管理员信息）
     * @param page 分页对象
     * @param status 状态筛选（可选）
     * @param uploaderType 上传者类型筛选（可选）
     * @param keyword 搜索关键字（可选）
     * @return 视频分页结果
     */
    IPage<Video> getVideosForAdmin(Page<?> page,
                                   @Param("status") Integer status,
                                   @Param("uploaderType") Integer uploaderType,
                                   @Param("keyword") String keyword);
    
    /**
     * 获取热门视频列表（按观看次数排序）
     * @param limit 限制数量
     * @return 热门视频列表
     */
    List<Video> getPopularVideos(@Param("limit") Integer limit);
    
    /**
     * 获取最新视频列表
     * @param limit 限制数量
     * @return 最新视频列表
     */
    List<Video> getLatestVideos(@Param("limit") Integer limit);
    
    /**
     * 获取视频统计信息
     * @return 统计数据
     */
    Map<String, Object> getVideoStatistics();
    
    /**
     * 增加视频观看次数
     * @param videoId 视频ID
     * @return 更新行数
     */
    int incrementViewCount(@Param("videoId") Long videoId);
    
    /**
     * 增加视频点赞次数
     * @param videoId 视频ID
     * @return 更新行数
     */
    int incrementLikeCount(@Param("videoId") Long videoId);
    
    /**
     * 减少视频点赞次数
     * @param videoId 视频ID
     * @return 更新行数
     */
    int decrementLikeCount(@Param("videoId") Long videoId);
} 