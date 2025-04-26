package com.example.controller;

import com.example.constants.Const;
import com.example.entity.Dto.Account;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.LoginVo;
import com.example.entity.Vo.request.RegisterVo;
import com.example.entity.Vo.request.ResetConfirmVo;
import com.example.entity.Vo.request.resetPasswordVo;
import com.example.entity.Vo.response.AuthorizeVo;
import com.example.service.userService;
import com.example.utils.ControllerUtils;
import com.example.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

//用户相关接口
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    ControllerUtils utils;
    
    @Autowired
    userService userservice;
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtUtils jwtUtils;
    @Qualifier("redisTemplate")
    @Autowired
    private RedisTemplate redisTemplate;

    //登录接口
    @PostMapping("/login")
    public RestBean<AuthorizeVo> UserLogin(@Valid  LoginVo vo, HttpServletRequest request){
        RestBean<AuthorizeVo> result = userservice.login(vo);

        // 如果登录成功，设置用户ID属性
        if(result.getCode() == 200 && result.getData() != null) {
            AuthorizeVo authorizeVo = result.getData();
            // 设置用户ID到request属性中
            request.setAttribute(Const.ATTR_USER_ID, authorizeVo.getId());
        }

        return result;
    }
    
    /**
     * 退出登录接口

     * @param authorization 请求头中的token
     * @return 操作结果
     */

    @GetMapping("/logout")
    public RestBean<Void> logout(HttpServletRequest request,
                                 @RequestHeader("Authorization") String authorization) {
        return userservice.logout(request,authorization);

    }
    //注册接口
    @PostMapping("/register")
    public RestBean<Void> UserRegister(@Valid @RequestBody RegisterVo vo){

        return utils.messageHandle(()->userservice.registerAccount(vo));
    }

    @GetMapping("/ask-code")
    public RestBean<String> askCode(@Email(message = "请输入正确的的电子邮件") @RequestParam String email,@RequestParam String type) {
       return userservice.askCode(email,type);
    }
    @GetMapping("/list")
    public RestBean UserList(){
        return RestBean.success("恭喜你,成功访问到受保护资源");
    }
    @PostMapping("/resetConfirm")
    public RestBean resetConfirm(@Valid @RequestBody ResetConfirmVo vo){

        return utils.messageHandle(()->
             userservice.confirmreset(vo)
        );
     }
     @PostMapping("/resetPassword")
     public RestBean resetpassword(@Valid @RequestBody resetPasswordVo vo){
        return utils.messageHandle(()->userservice.resetPassword(vo));
     }

}
