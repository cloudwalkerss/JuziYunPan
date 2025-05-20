package com.example.controller;


import com.example.service.Impl.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试邮件发送
 * @author qzz
 */
@RestController
@RequestMapping("/mail")
public class SendMailController {

    @Autowired
    MailService mailService;


    /**
     * 发送文本邮件
     * @param to
     * @param subject
     * @param text
     */
    @RequestMapping("/sendTextMail")
    public void sendTextMail(String to,String subject,String text){
        mailService.sendTextMailMessage(to,subject,text);
    }

    /**
     * 发送HTML邮件
     * @param to
     * @param subject
     * @param content
     */
    @RequestMapping("/sendHtmlMailMessage")
    public void sendHtmlMailMessage(String to,String subject,String content){
        mailService.sendHtmlMailMessage(to,subject,content);
    }
}

