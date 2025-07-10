package com.example.entity.Vo.response;

import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("use_space")
    Long useSpace;
   @TableField("total_space")
    Long totalSpace;
    @TableField("Banned")
    Integer Banned;
}
