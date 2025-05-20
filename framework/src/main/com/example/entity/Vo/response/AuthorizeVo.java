package com.example.entity.Vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AuthorizeVo {
    int id;
    String username;
    String role;

    String token;

    Date expireTime;

    String nickname;

}
