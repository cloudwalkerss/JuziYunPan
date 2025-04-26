package com.example.service.Impl;


import com.example.entity.RestBean;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 邮件业务类
 * @author qzz
 */
@Service
public class MailService {
    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    /**
     * 注入邮件工具类
     */
    @Autowired
    private JavaMailSenderImpl javaMailSender;

    @Value("${spring.mail.username}")
    private String sendMailer;

    /**
     * 检测邮件信息类
     * @param to
     * @param subject
     * @param text
     */
    private void checkMail(String to,String subject,String text){
        if(to.isEmpty()){
            throw new RuntimeException("邮件收信人不能为空");
        }
        if(subject.isEmpty()){
            throw new RuntimeException("邮件主题不能为空");
        }
        if(text.isEmpty()){
            throw new RuntimeException("邮件内容不能为空");
        }
    }

    /**
     * 发送纯文本邮件
     * @param to
     * @param subject
     * @param text
     */
    public Boolean sendTextMailMessage(String to,String subject,String text){

        try {
            //true 代表支持复杂的类型
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(javaMailSender.createMimeMessage(),true);
            //邮件发信人
            mimeMessageHelper.setFrom(sendMailer);
            //邮件收信人  1或多个
            mimeMessageHelper.setTo(to.split(","));
            //邮件主题
            mimeMessageHelper.setSubject(subject);
            //邮件内容
            mimeMessageHelper.setText(text);
            //邮件发送时间
            mimeMessageHelper.setSentDate(new Date());

            //发送邮件
            javaMailSender.send(mimeMessageHelper.getMimeMessage());
            System.out.println("发送邮件成功："+sendMailer+"->"+to);

        } catch (Exception e) {

            e.printStackTrace();
            System.out.println("发送邮件失败："+e.getMessage());
            System.out.println(e.getMessage());
           return  false;
        }
        return  true;
    }

    /**
     * 发送html邮件
     * @param to
     * @param subject
     * @param content
     */
    public Boolean sendHtmlMailMessage(String to,String subject,String content){

        content="<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta charset=\"utf-8\">\n" +
                "<title>邮件</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "\t<h3>这是一封HTML邮件！</h3>\n" +
                "</body>\n" +
                "</html>";
        try {
            //true 代表支持复杂的类型
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(javaMailSender.createMimeMessage(),true);
            //邮件发信人
            mimeMessageHelper.setFrom(sendMailer);
            //邮件收信人  1或多个
            mimeMessageHelper.setTo(to.split(","));
            //邮件主题
            mimeMessageHelper.setSubject(subject);
            //邮件内容   true 代表支持html
            mimeMessageHelper.setText(content,true);
            //邮件发送时间
            mimeMessageHelper.setSentDate(new Date());

            //发送邮件
            javaMailSender.send(mimeMessageHelper.getMimeMessage());
            System.out.println("发送邮件成功："+sendMailer+"->"+to);

        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("发送邮件失败："+e.getMessage());
            return  false;
        }
        return true;
    }

    /**
     * 发送精美验证码HTML邮件
     * @param to 收件人
     * @param subject 邮件主题
     * @param verificationCode 验证码
     * @return 发送结果
     */
    public Boolean sendVerificationCodeEmail(String to, String subject, String verificationCode) {
        // 判断是密码重置还是注册验证
        boolean isPasswordReset = subject.contains("重置") || subject.contains("密码");
        
        // 邮件标题
        String title = isPasswordReset ? "重置密码验证" : "账号注册验证";
        // 邮件操作类型
        String actionType = isPasswordReset ? "重置密码" : "注册账号";
        // 邮件内容说明
        String description = isPasswordReset ? 
                "您正在进行橘子云盘账号密码重置操作，请使用下方验证码完成验证。" : 
                "感谢您注册橘子云盘，请使用下方验证码完成注册。";
        // 邮件注意事项
        String notice = "验证码有效期为1分钟，请勿将验证码告知他人。";

        String content = "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + title + "</title>\n" +
                "</head>\n" +
                "<body style=\"margin: 0; padding: 0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f7f7f7;\">\n" +
                "    <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f7f7f7;\">\n" +
                "        <tr>\n" +
                "            <td align=\"center\" style=\"padding: 40px 0;\">\n" +
                "                <table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); overflow: hidden;\">\n" +
                "                    <!-- 头部橘色横幅 -->\n" +
                "                    <tr>\n" +
                "                        <td style=\"background: linear-gradient(to right, #FF9500, #FF5722); padding: 30px 40px; text-align: center;\">\n" +
                "                            <h1 style=\"color: white; margin: 0; font-size: 28px; font-weight: 500;\">橘子云盘 - " + title + "</h1>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                    \n" +
                "                    <!-- 主体内容 -->\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding: 40px;\">\n" +
                "                            <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                "                                <tr>\n" +
                "                                    <td>\n" +
                "                                        <h2 style=\"color: #333333; font-size: 20px; margin: 0 0 20px 0;\">尊敬的用户：</h2>\n" +
                "                                        <p style=\"color: #555555; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;\">" + description + "</p>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                                \n" +
                "                                <!-- 验证码区域 -->\n" +
                "                                <tr>\n" +
                "                                    <td style=\"padding: 20px 0;\">\n" +
                "                                        <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                "                                            <tr>\n" +
                "                                                <td align=\"center\" style=\"background-color: #f9f9f9; padding: 30px; border-radius: 8px; border: 1px dashed #dddddd;\">\n" +
                "                                                    <p style=\"color: #888888; font-size: 14px; margin: 0 0 10px 0;\">您的验证码为</p>\n" +
                "                                                    <h1 style=\"color: #FF9500; font-size: 36px; letter-spacing: 5px; margin: 0 0 10px 0; font-weight: 700;\">" + verificationCode + "</h1>\n" +
                "                                                    <p style=\"color: #888888; font-size: 14px; margin: 0;\">" + notice + "</p>\n" +
                "                                                </td>\n" +
                "                                            </tr>\n" +
                "                                        </table>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                                \n" +
                "                                <!-- 图标区域 -->\n" +
                "                                <tr>\n" +
                "                                    <td style=\"padding: 20px 0; text-align: center;\">\n" +
                "                                        <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                "                                            <tr>\n" +
                "                                                <td width=\"33%\" align=\"center\">\n" +
                "                                                    <div style=\"width: 60px; height: 60px; border-radius: 50%; background-color: #FFF3E0; margin: 0 auto; display: flex; align-items: center; justify-content: center;\">\n" +
                "                                                        <span style=\"font-size: 30px;\">🔒</span>\n" +
                "                                                    </div>\n" +
                "                                                    <p style=\"color: #666666; font-size: 14px; margin: 10px 0 0 0;\">安全保障</p>\n" +
                "                                                </td>\n" +
                "                                                <td width=\"33%\" align=\"center\">\n" +
                "                                                    <div style=\"width: 60px; height: 60px; border-radius: 50%; background-color: #E3F2FD; margin: 0 auto; display: flex; align-items: center; justify-content: center;\">\n" +
                "                                                        <span style=\"font-size: 30px;\">🚀</span>\n" +
                "                                                    </div>\n" +
                "                                                    <p style=\"color: #666666; font-size: 14px; margin: 10px 0 0 0;\">高速体验</p>\n" +
                "                                                </td>\n" +
                "                                                <td width=\"33%\" align=\"center\">\n" +
                "                                                    <div style=\"width: 60px; height: 60px; border-radius: 50%; background-color: #E8F5E9; margin: 0 auto; display: flex; align-items: center; justify-content: center;\">\n" +
                "                                                        <span style=\"font-size: 30px;\">📱</span>\n" +
                "                                                    </div>\n" +
                "                                                    <p style=\"color: #666666; font-size: 14px; margin: 10px 0 0 0;\">随时访问</p>\n" +
                "                                                </td>\n" +
                "                                            </tr>\n" +
                "                                        </table>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                                \n" +
                "                                <!-- 提示文本 -->\n" +
                "                                <tr>\n" +
                "                                    <td style=\"padding: 20px 0;\">\n" +
                "                                        <p style=\"color: #888888; font-size: 14px; line-height: 1.5; margin: 0; padding: 15px; background-color: #FFFDE7; border-radius: 4px; border-left: 4px solid #FFC107;\">\n" +
                "                                            <strong>安全提示：</strong> 如非本人操作，请忽略此邮件。为保障账号安全，请勿将验证码泄露给他人。\n" +
                "                                        </p>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                            </table>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                    \n" +
                "                    <!-- 底部 -->\n" +
                "                    <tr>\n" +
                "                        <td style=\"background-color: #f9f9f9; padding: 20px 40px; text-align: center; border-top: 1px solid #eeeeee;\">\n" +
                "                            <p style=\"color: #999999; font-size: 13px; margin: 0 0 10px 0;\">© 2024 橘子云盘 版权所有</p>\n" +
                "                            <p style=\"color: #999999; font-size: 12px; margin: 0;\">此邮件由系统自动发送，请勿直接回复</p>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                </table>\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "    </table>\n" +
                "</body>\n" +
                "</html>";

        try {
            //true 代表支持复杂的类型
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(javaMailSender.createMimeMessage(), true);
            //邮件发信人
            mimeMessageHelper.setFrom(sendMailer);
            //邮件收信人  1或多个
            mimeMessageHelper.setTo(to.split(","));
            //邮件主题
            mimeMessageHelper.setSubject(subject);
            //邮件内容   true 代表支持html
            mimeMessageHelper.setText(content, true);
            //邮件发送时间
            mimeMessageHelper.setSentDate(new Date());

            //发送邮件
            javaMailSender.send(mimeMessageHelper.getMimeMessage());
            System.out.println("发送验证码HTML邮件成功：" + sendMailer + "->" + to);

        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("发送验证码HTML邮件失败：" + e.getMessage());
            return false;
        }
        return true;
    }
}

