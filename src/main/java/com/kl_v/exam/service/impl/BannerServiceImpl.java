package com.kl_v.exam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kl_v.exam.entity.Banner;
import com.kl_v.exam.mapper.BannerMapper;
import com.kl_v.exam.service.BannerService;
import com.kl_v.exam.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
 * 轮播图服务实现类
 */
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    @Autowired
    private FileUploadService fileUploadService;
    @Override
    public String uploadBannerImage(MultipartFile file) throws Exception{
        //非空校验
        if (file ==null || file.isEmpty()){
            throw new RuntimeException("上传轮播图图片失败！原因：上传的文件为空！！");
        }
        //格式校验
        if (!file.getContentType().startsWith("image")) {
            throw new RuntimeException("上传轮播图图片失败！原因：上传的文件类型错误！不是图片");
        }
        //调用核心业务
        String url = fileUploadService.uploadFile("banners", file);
        //返回地址

        return url;
    }

    @Override
    public void addBanner(Banner banner) throws Exception {
        //1.检查默认值 isActive = true sortOrder = 1
        if(banner.getIsActive() == null){
            banner.setIsActive(Boolean.TRUE);
        }
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(5);
        }
        //2.createTime updateTime isDeleted前端一定不传值
//        banner.setCreateTime(new Date());
//        banner.setUpdateTime(new Date());
//        banner.setIsDeleted((byte) 0);
        //3.继续保存banner
        boolean saved = save(banner);
        if (!saved){
            throw new Exception("轮播图数据保存失败！！");
        }


    }

    @Override
    public void updateBanner(Banner banner) {
        //1.检查默认值 isActive = true sortOrder = 1
        if(banner.getIsActive() == null){
            banner.setIsActive(Boolean.TRUE);
        }
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(5);
        }

        //2.调用修改即可
        boolean updated = updateById(banner);
        if(!updated){
            throw new RuntimeException("轮播图修改失败");
        }
    }
}