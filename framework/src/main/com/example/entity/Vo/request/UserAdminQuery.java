package com.example.entity.Vo.request;

import lombok.Data;

import java.util.Date;
//请求参数
@Data
public class UserAdminQuery {

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

    private Integer pageNo;
    private Integer pageSize;
    private String orderBy;
}
