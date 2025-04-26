package com.example.entity.Vo.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data

public class RegisterVo {
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$")
    @Length(min = 6, max = 20)//要求6到20维包括数字和英文的用户名
     private String username;
    @Length(min = 8, max = 20)
     private String password;

    @Length(min = 8, max = 20)
    private String repeatpassword;

     @Email
     private String email;
    @Length(min = 6, max = 6)
    private String code;

}
