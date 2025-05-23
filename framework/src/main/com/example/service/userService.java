package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.Dto.Account;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.LoginVo;
import com.example.entity.Vo.request.RegisterVo;
import com.example.entity.Vo.request.ResetConfirmVo;
import com.example.entity.Vo.request.resetPasswordVo;
import com.example.entity.Vo.response.AuthorizeVo;
import org.apache.ibatis.annotations.Mapper;

import jakarta.servlet.http.HttpServletResponse;


public interface userService extends IService<Account> {


     void updateUserInfoByUserId(Account userInfo, Integer userId) ;


    String registerAccount(RegisterVo vo);

    RestBean<AuthorizeVo> login(LoginVo vo);

     RestBean<String> askCode(String email,String type);

    String confirmreset(ResetConfirmVo vo);

    String resetPassword(resetPasswordVo vo);

    RestBean<Void> logout( String authorization);

   void getavatar(HttpServletResponse response, Integer userId);

    RestBean getuserinfo(Integer userId);


}
