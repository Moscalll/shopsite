package com.example.shopsite.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileUploadService {
    /**
     * 上传图片文件
     * @param file 上传的文件
     * @return 返回文件的访问路径（相对于web根目录）
     * @throws IOException 文件操作异常
     */
    String uploadImage(MultipartFile file) throws IOException;
    
    /**
     * 删除图片文件
     * @param imagePath 图片路径
     * @throws IOException 文件操作异常
     */
    void deleteImage(String imagePath) throws IOException;
}




















