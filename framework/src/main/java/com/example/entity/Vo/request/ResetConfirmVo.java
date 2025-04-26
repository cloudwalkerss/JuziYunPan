package com.example.entity.Vo.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ResetConfirmVo {
     @Email
     String email;
     @Length(min = 6, max = 6)
     String code;
}
