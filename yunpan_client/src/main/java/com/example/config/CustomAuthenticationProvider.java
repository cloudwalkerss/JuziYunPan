package com.example.config;

import com.example.constants.Const;
import com.example.entity.Dto.Account;
import com.example.entity.RestBean;
import com.example.mapper.UserMapper;
import com.example.utils.JwtUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;
    @Qualifier("redisTemplate")
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {


        // 获取用户名和密码//也可以是邮箱
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        // 直接查询数据库验证用户
        Account account = userMapper.findByUsername(username);
        Account accountByEmail = userMapper.findByEmail(username);
        if (account == null&&accountByEmail==null) {
            throw new BadCredentialsException("用户不存在");
        }
        if(account==null){
            account=accountByEmail;
        }

        // 验证密码 (实际项目中应该使用加密方式)
        if (!account.getPassword().equals(password)) {
            throw new BadCredentialsException("密码错误");
        }

        // 生成JWT令牌
        String token = jwtUtils.createJwt(account.getUsername(), account.getId());

        // 创建认证结果
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                username, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + account.getRole())));

        // 设置额外信息
        Map<String, Object> details = new HashMap<>();
        details.put("token", token);
        details.put("userId", account.getId());
        details.put("role", account.getRole());
        details.put("nickname", account.getNickname());
        authToken.setDetails(details);

        return authToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}