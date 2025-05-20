package com.example.entity.Vo.request;

import jakarta.validation.constraints.Email;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class resetPasswordVo {
    @Email
    private String email;
    @Length(min = 8, max = 20)
     String newPassword;
    @Length(min = 8, max = 20)
     String repeatNewPassword;
}
