package com.kl_v.exam.service.impl;

import com.kl_v.exam.config.properties.MinioProperties;
import com.kl_v.exam.service.FileUploadService;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private MinioProperties minioProperties;
    @Override
    public String uploadFile(String folder, MultipartFile file) throws Exception{
        //1.判断桶是否存在
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
        //2.不存在，创建桶。同时设置访问权限
        if (!bucketExists){
            //创建桶
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
            String config = """
                    {
                      "Version": "2012-10-17",
                      "Statement": [{
                        "Effect": "Allow",
                        "Principal": "*",
                        "Action": ["s3:GetObject"],
                        "Resource": ["arn:aws:s3:::%s/*"]
                      }]
                    }
                    """.formatted(minioProperties.getBucketName());
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .config(config)
                    .build());
        }
        //3.处理上传的对象名（影响，minio桶中的文件结构）
        //解决覆盖问题：确保对象和文件的名字唯一即可
        String objectName = folder+"/"+new SimpleDateFormat("yyyyMMddHH").format(new Date())+"/"+
                UUID.randomUUID().toString().replaceAll("-","")+"_"+file.getOriginalFilename();

        log.debug("文件上传核心业务方法，处理后的文锦对象名：{}",objectName);

        //4.上传文件 putObject方法
        minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .contentType(file.getContentType())
                        .object(objectName)//对象名
                        .stream(file.getInputStream(),file.getSize(),-1)//-1是让minio自动处理文件大小
                .build());

        //5.拼接回显地址[端点+桶+对象名]
        String url = String.join("/", minioProperties.getEndpoint(), minioProperties.getBucketName(), objectName);
        log.info("文件上传核心业务，完成文件上传，返回地址为：{}",objectName,url);
        return url;
    }
}
