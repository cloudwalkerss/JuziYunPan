package com.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.example.mapper")
public class YunpanClientApplication {

    public static void main(String[] args) {

        SpringApplication.run(YunpanClientApplication.class, args);
    }

}
