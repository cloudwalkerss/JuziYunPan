package com.example.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cors解决跨域问题
 */
//@Configuration
//public class CorsConfig implements WebMvcConfigurer {
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//            .allowedOrigins("http://localhost:5173")
//            .allowedMethods("GET", "POST", "Options", "DELETE", "PUT")
//            .exposedHeaders("Captcha-Id", "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials")
//            .allowedHeaders("*")
//            .allowCredentials(true)
//            .maxAge(16800);
//    }
//}

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // 使用模式匹配而不是具体的origin
                .allowedMethods("GET", "POST", "OPTIONS", "DELETE", "PUT")
                .exposedHeaders("*")  // 暴露所有头部信息
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(16800);
    }
}