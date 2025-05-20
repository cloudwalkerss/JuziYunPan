package com.example.service.Impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.constants.AppConfig;
import com.example.constants.Constants;
import com.example.entity.Dto.Account;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.LoginVo;
import com.example.entity.Vo.request.RegisterVo;
import com.example.entity.Vo.request.ResetConfirmVo;
import com.example.entity.Vo.request.resetPasswordVo;
import com.example.entity.Vo.response.AuthorizeVo;
import com.example.entity.Vo.response.UserInfoVo;
import com.example.mapper.UserMapper;
import com.example.service.userService;
import com.example.utils.BeanCopyUtils;
import com.example.constants.Const;
import com.example.utils.JwtUtils;
import com.example.utils.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;


import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.example.constants.Const.VERIFY_EMAIL_DATA;
import static com.example.service.Impl.MailService.logger;

@Service("userService")
public class userServiceimpl extends ServiceImpl<UserMapper, Account> implements userService {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";
    @Resource
    UserMapper userMapper;
    @Resource
    BeanCopyUtils beanCopyUtils;
    @Resource
    SnowflakeIdGenerator idGenerator;
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
     MailService mailService;
    @Autowired
    AppConfig appConfig;

    @Autowired
    FileServiceImpl fileService;
   //更新头像
    @Override
    public void updateUserInfoByUserId(Account userInfo, Integer userId) {
         userInfo.setId(userId);
         userMapper.updateById(userInfo);
    }

    @Override
    public String registerAccount(RegisterVo vo) {


        // 验证验证码
        String key = VERIFY_EMAIL_DATA + vo.getEmail();

        String correctCode = redisTemplate.opsForValue().get(key);
        
        if (correctCode == null) {
            return "验证码已过期，请重试";
        }

        if (!correctCode.equals(vo.getCode())) {

            return "验证码错误，请重新输入";
        }

        // 验证成功后删除验证码，防止重复使用
        redisTemplate.delete(key);
          
        //判断一下username是否重复
        Account account=userMapper.findByUsername(vo.getUsername());
        if(account!=null){
            return "该账号已经被注册，请更换其他";
        }
        //判断一下邮箱是否已经注册过了
        Account accountByEmail=userMapper.findByEmail(vo.getEmail());
        if(accountByEmail!=null){
            return "该邮箱已经被注册，请更换其他邮箱";
        }
        
        //转化为实体类
        Account user = beanCopyUtils.copyBean(vo, Account.class);
        //补充信息
        long uuid= idGenerator.nextId();
        user.setAvatar("默认地址");
        user.setNickname("用户"+uuid);
        user.setRole("0");
        user.setRegisterTime(new Date());
        user.setUpdateTime(new Date());
        user.setUseSpace(0L);
        
        //如果没问题就将账户插入
         userMapper.createAccount(user);
        return null;
    }

    @Override
    public RestBean<AuthorizeVo> login(LoginVo vo) {
        try {

            // 验证验证码
            String captchaKey = "captcha:" + vo.getCaptchaId();
            String correctCaptcha = redisTemplate.opsForValue().get(captchaKey);
            
            if (correctCaptcha == null) {
                return RestBean.failure(403, "验证码已过期，请刷新验证码");
            }
            
            if (!correctCaptcha.equalsIgnoreCase(vo.getCaptcha())) {
                return RestBean.failure(403, "验证码错误，请重新输入");
            }
            
            // 验证成功后删除验证码，防止重复使用
            redisTemplate.delete(captchaKey);

            // 创建认证令牌
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    vo.getUsername(),
                    vo.getPassword()
            );

            // 进行认证（这里会调用CustomAuthenticationProvider的authenticate方法）
            Authentication result = authenticationManager.authenticate(authentication);//令牌在这一步已经颁发完毕了
            // 从认证结果中获取用户信息
            Map<String, Object> details = (Map<String, Object>) result.getDetails();
            Integer userId = (Integer) details.get("userId");
            String token=(String) details.get("token");
            // 检查用户是否已经登录
            String loginStatusKey = Const.USER_LOGIN_STATUS + userId;
            Boolean isLoggedIn = redisTemplate.hasKey(loginStatusKey);

            if (Boolean.TRUE.equals(isLoggedIn)) {
               //让先前的令牌失效
                String jwt=redisTemplate.opsForValue().get(loginStatusKey);
                jwtUtils.invalidateJwt(jwt);

            }
           ;
            redisTemplate.opsForValue().set(loginStatusKey,token, jwtUtils.expireTime().getTime() - new Date().getTime(), TimeUnit.MILLISECONDS);



            String role = (String) details.get("role");

            String nickname = (String) details.get("nickname");

            // 创建返回对象
            AuthorizeVo authorizeVo = new AuthorizeVo();
            authorizeVo.setId(userId);
            authorizeVo.setUsername(vo.getUsername());
            authorizeVo.setRole(role);
            authorizeVo.setToken(token);
            authorizeVo.setExpireTime(jwtUtils.expireTime());
            authorizeVo.setNickname(nickname);

            return RestBean.success(authorizeVo);
        } catch (BadCredentialsException e) {
            return RestBean.failure(402, "用户名或者密码错误");
        }
    }

    @Override
    public RestBean<String> askCode(String email, String type) {
        // 检查是否在请求冷却期内
        String cooldownKey = "email-cooldown:" + email;
        Boolean inCooldown = redisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(inCooldown)) {
            Long ttl = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            return RestBean.failure(429, "请求过于频繁，请在" + ttl + "秒后再试");
        }

        // 判断以下是否有这个用户
        Account account = userMapper.findByEmail(email);
        if (account == null && type.equals("reset")) {
            return RestBean.failure(403, "没有人注册此邮箱");
        }
        if (account != null && type.equals("register")) {
            // 说明注册过了
            return RestBean.failure(403, "已经有人注册过该邮箱");
        }

        // 选择6位数字
        int code = new Random().nextInt(900000) + 100000;
        
        // 设置验证码冷却期，60秒内不能再次请求
        redisTemplate.opsForValue().set(cooldownKey, "cooldown", 60, TimeUnit.SECONDS);
        
        // 验证码放到redis里面
        if (Objects.equals(type, "register"))
            redisTemplate.opsForValue()
                    .set(VERIFY_EMAIL_DATA + email, String.valueOf(code), 1, TimeUnit.MINUTES);
        if (Objects.equals(type, "reset")) {
            redisTemplate.opsForValue()
                    .set(Const.VERIFY_EMAIL_RESET + email, String.valueOf(code), 1, TimeUnit.MINUTES);
        }
        
        // 将验证码发邮件发出去
        boolean is;
        try {
            // 根据类型确定邮件主题
            String subject = Objects.equals(type, "register") ? 
                    "橘子云盘账号注册验证码" : "橘子云盘密码重置验证码";
            
            // 使用HTML格式发送验证码邮件
            is = mailService.sendVerificationCodeEmail(email, subject, String.valueOf(code));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return RestBean.failure(404, "邮件发送失败");
        }
        if (!is) {
            return RestBean.failure(404, "邮件发送失败");
        }

        return RestBean.success("邮件已经成功发送");
    }

    @Override
    public String confirmreset(ResetConfirmVo vo) {
        //判断是否有这个用户
        Account account=userMapper.findByEmail(vo.getEmail());
        if(account==null){
            return "该邮件并未注册";
        }
        String key=Const.VERIFY_EMAIL_RESET + vo.getEmail();
        //判断验证码是否正确
        String correctCode=redisTemplate.opsForValue().get(key);
        if(!vo.getCode().equals(correctCode)){
            return "验证码错误";
        }


        return null;
    }

    @Override
    public String resetPassword(resetPasswordVo vo) {
        if(!vo.getNewPassword().equals(vo.getRepeatNewPassword())){
            return "请按照规范设置密码";
        }
        try {
            userMapper.updatePassword(vo.getEmail(),vo.getNewPassword());
        }catch (Exception e){
            System.out.println(e.getMessage());
            return "修改失败";
        }

        return null;
    }

    @Override
    public RestBean<Void> logout(String authorization) {
        DecodedJWT jwt=null;
        try {
             jwt = jwtUtils.resolveJwt(authorization);
        }catch (Exception e){
            System.out.println(e.getMessage());
            return RestBean.failure(406,"解析token失败");
        }

        int userId=-1;
        if (jwt != null) {
            userId = jwtUtils.toId(jwt);
            redisTemplate.delete(Const.USER_LOGIN_STATUS + userId);
        }



        // 使用JwtUtils将当前token加入黑名单，使其失效
        if (jwtUtils.invalidateJwt(authorization)) {
            return RestBean.success();
        } else {
            return RestBean.failure(400, "退出登录失败，请重试");
        }
    }
     //获取用户头像
    @Override
    public void getavatar(HttpServletResponse response, Integer userId) {
        String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File folder = new File(appConfig.getProjectFolder() + avatarFolderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String avatarPath = appConfig.getProjectFolder() + avatarFolderName + userId + Constants.AVATAR_SUFFIX;
        File file = new File(avatarPath);
        if (!file.exists()) {
            if (!new File(appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT).exists()) {
                printNoDefaultImage(response);
                return;
            }
            avatarPath = appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT;
        }
        response.setContentType("image/jpg");
        fileService.readFile(response, avatarPath);
    }
    private void printNoDefaultImage(HttpServletResponse response) {
        response.setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
            writer.close();
        } catch (Exception e) {
            logger.error("输出无默认图失败", e);
        } finally {
            writer.close();
        }
    }


    @Override
    public RestBean getuserinfo(Integer userId) {
           Account account = userMapper.findById(userId);

            if(account==null){
                return RestBean.failure(402,"不存在此用户");

            }


        UserInfoVo vo = beanCopyUtils.copyBean(account, UserInfoVo.class);
            return RestBean.success(vo);


    }


}

