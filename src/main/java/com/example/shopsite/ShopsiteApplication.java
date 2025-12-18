package com.example.shopsite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShopsiteApplication {

    static {
        // 在类加载时设置系统属性（在 Spring 容器和 Tomcat 启动前）
        // 禁用参数数量限制（-1 表示无限制）
        System.setProperty("server.tomcat.max-parameter-count", "-1");
        
        // 设置最大 POST 大小：50MB
        System.setProperty("server.tomcat.max-http-form-post-size", "52428800");
        
        // 设置最大 Swallow 大小：50MB
        System.setProperty("server.tomcat.max-swallow-size", "52428800");
    }
	
    public static void main(String[] args) {
        SpringApplication.run(ShopsiteApplication.class, args);
    }
}
