package com.example.entity.Vo.request;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;
//请求参数
@Data
public class UserAdminQuery {

    Integer id;

    String username;
    @TableField("nick_name")
    String nickname;


    String email;

    String role;

    String avatar;

    Date registerTime;

    Date updateTime;
    @TableField("use_space")
    Long useSpace;
    @TableField("total_space")
    Long totalSpace;
    @TableField("banned")
    Integer Banned;

    private Integer pageNo;
    private Integer pageSize;
    private String orderBy;
}
