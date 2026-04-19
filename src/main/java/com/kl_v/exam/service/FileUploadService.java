package com.kl_v.exam.service;


import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务
 * 支持MinIO和本地文件存储两种方式
 */

public interface FileUploadService {

    /**
     * 实现文件上传的核心业务方法！
     * @param folder 不同上传位置的文件夹
     * @param file 上传文件的封装对象
     * @return 可以访问的文件地址
     */

    String uploadFile(String folder, MultipartFile file) throws Exception;

} 