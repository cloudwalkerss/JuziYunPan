package com.example.entity.Dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 数据库中的用户信息
 */
@Data
@TableName("account")
@NoArgsConstructor
@AllArgsConstructor
public class Account  {
    @TableId(type = IdType.AUTO)
    Integer id;

    String username;

    @TableField("nick_name")
    String nickname;

    String password;

    String email;

    String role;

    String avatar;

    Date registerTime;

    Date updateTime;
    @TableField("use_space")
    Long useSpace;
    @TableField("total_space")
    Long totalSpace;


    @TableField("Banned")
    Integer Banned;//账号封禁
}