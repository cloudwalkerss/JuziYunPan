package com.example.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.constants.Const;
import com.example.entity.Dto.Account;
import com.example.mapper.UserMapper;
import com.example.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Resource
    private JwtUtils jwtUtils;
    
    @Resource
    private UserMapper userMapper;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String token = getJwtFromRequest(request);
        
        if (token != null) {
            // 解析JWT
            DecodedJWT jwt = jwtUtils.resolveJwt(token);
            
            if (jwt != null) {
                // 从JWT获取用户ID
                Integer userId = jwtUtils.toId(jwt);
                
                // 获取用户信息
                Account account = userMapper.findById(userId);
                
                if (account != null) {
                    // 创建认证信息
                    List<GrantedAuthority> authorities =
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + account.getRole()));
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(account.getUsername(), null, authorities);
                    
                    // 设置认证详情
                    Map<String, Object> details = new HashMap<>();
                    details.put("userId", account.getId());
                    details.put("role", account.getRole());
                    authentication.setDetails(details);
                    
                    // 设置安全上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                }
            }
        }
        
        filterChain.doFilter(request, response);//直接放行因为如果没有通过验证将会被拦截
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken;
        }
        return null;
    }
}