package com.example.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KaptchaConfig {

    @Bean
    public DefaultKaptcha defaultKaptcha() {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        // 图片宽度
        properties.setProperty("kaptcha.image.width", "150");
        // 图片高度
        properties.setProperty("kaptcha.image.height", "50");
        // 字体大小
        properties.setProperty("kaptcha.textproducer.font.size", "32");
        // 字体颜色
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        // 字符集 - 改为纯数字，避免大小写问题
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789");
        // 验证码长度
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        // 干扰实现类
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.DefaultNoise");
        // 干扰颜色
        properties.setProperty("kaptcha.noise.color", "black");
        // 边框
        properties.setProperty("kaptcha.border", "yes");
        // 边框颜色
        properties.setProperty("kaptcha.border.color", "105,179,90");
        // 边框厚度
        properties.setProperty("kaptcha.border.thickness", "1");
        // 图片样式 - 使用更简单的样式，提高可读性
        properties.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.ShadowGimpy");
        // 背景颜色渐变开始
        properties.setProperty("kaptcha.background.clear.from", "white");
        // 背景颜色渐变结束
        properties.setProperty("kaptcha.background.clear.to", "white");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
} 