package com.example.controller;

import com.example.constants.AppConfig;
import com.example.constants.Const;
import com.example.constants.Constants;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.LoginVo;
import com.example.entity.Vo.request.RegisterVo;
import com.example.entity.Vo.request.ResetConfirmVo;
import com.example.entity.Vo.request.resetPasswordVo;
import com.example.entity.Vo.response.AuthorizeVo;
import com.example.service.userService;
import com.example.utils.ControllerUtils;
import com.example.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;
import com.example.component.RedisComponent;
import com.example.entity.Dto.UserSpaceDTO;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

import java.io.File;

import static com.example.service.Impl.MailService.logger;

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

    @Autowired
    private RedisComponent redisComponent;

    @Resource
    private AppConfig appConfig;

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
    public RestBean<Void> logout(
                                 @RequestHeader("Authorization") String authorization) {
        return userservice.logout(authorization);

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
     @GetMapping("/getAvatar/{userid}")
    public void getAvatar(HttpServletResponse response, @PathVariable("userid") Integer userId){

         userservice.getavatar(response,userId);
     }

     @GetMapping("/getUserInfo")
    public RestBean getinfo(@RequestAttribute (Const.ATTR_USER_ID) Integer userId){
        return  userservice.getuserinfo(userId);
     }


    @RequestMapping("/updateUserAvatar")
    public RestBean updateUserAvatar(@RequestAttribute (Const.ATTR_USER_ID) Integer userId, MultipartFile avatar) {

        String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        if (!targetFileFolder.exists()) {
            targetFileFolder.mkdirs();
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" + userId + Constants.AVATAR_SUFFIX);
        try {
            avatar.transferTo(targetFile);//将上传的文件传到这个路径
        } catch (Exception e) {
            logger.error("上传头像失败", e);
        }

//       Account userInfo=new Account();
//
//        userservice.updateUserInfoByUserId(userInfo, userId);

        return RestBean.success();
    }



    /**
     * 获取用户空间使用情况
     * @param userId 用户ID
     * @return 用户空间使用数据
     */
    @RequestMapping("/getUseSpace")
    public RestBean<UserSpaceDTO> getUseSpace(@RequestAttribute(Const.ATTR_USER_ID) Integer userId) {
        // 调用Redis组件获取用户空间使用情况
        UserSpaceDTO userSpaceDTO = redisComponent.getUserSpaceUse(userId);
        return RestBean.success(userSpaceDTO);
    }

    /**
     * 刷新用户空间使用情况
     * @param userId 用户ID
     * @return 最新的用户空间使用数据
     */
    @RequestMapping("/refreshUserSpace")
    public RestBean<UserSpaceDTO> refreshUserSpace(@RequestAttribute(Const.ATTR_USER_ID) Integer userId) {
        // 使用RedisComponent的resetUserSpaceUse方法强制刷新用户空间信息
        UserSpaceDTO userSpaceDTO = redisComponent.resetUserSpaceUse(userId);
        return RestBean.success(userSpaceDTO);
    }

}
