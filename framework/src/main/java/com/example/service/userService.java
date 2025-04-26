package com.example.service;

import com.example.entity.RestBean;
import com.example.entity.Vo.request.LoginVo;
import com.example.entity.Vo.request.RegisterVo;
import com.example.entity.Vo.request.ResetConfirmVo;
import com.example.entity.Vo.request.resetPasswordVo;
import com.example.entity.Vo.response.AuthorizeVo;
import jakarta.servlet.http.HttpServletRequest;

public interface userService {




    String registerAccount(RegisterVo vo);

    RestBean<AuthorizeVo> login(LoginVo vo);

     RestBean<String> askCode(String email,String type);

    String confirmreset(ResetConfirmVo vo);

    String resetPassword(resetPasswordVo vo);

    RestBean<Void> logout(HttpServletRequest request, String authorization);
}
