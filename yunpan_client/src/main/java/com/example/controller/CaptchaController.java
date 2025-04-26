package com.example.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    @Autowired
    private DefaultKaptcha defaultKaptcha;
    
    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/generate")
    public void generateCaptcha(HttpServletResponse response) throws IOException {
        // 设置响应头
        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("image/jpeg");

        // 生成验证码文本
        String captchaText = defaultKaptcha.createText();
        
        // 生成唯一标识
        String captchaId = UUID.randomUUID().toString();
        
        // 将验证码存入Redis，设置有效期为5分钟
        redisTemplate.opsForValue().set("captcha:" + captchaId, captchaText, 5, TimeUnit.MINUTES);
        
        // 将唯一标识写入响应头
        response.setHeader("Captcha-Id", captchaId);
        
        // 根据文本生成验证码图片
        BufferedImage image = defaultKaptcha.createImage(captchaText);
        
        // 写入响应输出流
        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(image, "jpg", out);
        out.flush();
    }
} 