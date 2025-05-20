package com.example.entity.Vo.response;

import lombok.Data;

import java.util.Date;
@Data
public class UserAdminV0 {
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

    Boolean banned;
}
