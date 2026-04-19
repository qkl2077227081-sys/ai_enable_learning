package com.kl_v.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kl_v.exam.entity.Banner;
import org.springframework.web.multipart.MultipartFile;

/**
 * 轮播图服务接口
 */
public interface BannerService extends IService<Banner> {

    /**
     *上传轮播图业务 做核心校验+调用核心上传业务的方法
     * @param file 上传的文件
     * @return 回显的地址，失败就抛异常
     */
    String uploadBannerImage(MultipartFile file) throws Exception;

    /**
     * 保存轮播图信息
     * @param banner 轮播图信息对象
     */
    void addBanner(Banner banner) throws Exception;

    /**
     * 修改轮播图信息
     * @param banner
     */
    void updateBanner(Banner banner);

}