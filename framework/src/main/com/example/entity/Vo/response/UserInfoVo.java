package com.example.entity.Vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoVo {
    Integer id;

    String username;

    String nickname;

    String email;

    String role;

    String avatar;

    Date registerTime;

    Date updateTime;

     Long userspace;

    Long totalspace;



}
