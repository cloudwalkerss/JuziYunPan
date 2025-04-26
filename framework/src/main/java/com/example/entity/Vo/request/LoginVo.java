package com.example.entity.Vo.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginVo {

    @Length(min = 6, max = 20)//要求6到20维包括数字和英文的用户名

    private String username;
    @Length(min = 8, max = 20)
    private String password;
    
    @Length(min = 4, max = 6)
    private String captcha;
    
    private String captchaId;
    //TODO后面可以搞一个验证码

}
