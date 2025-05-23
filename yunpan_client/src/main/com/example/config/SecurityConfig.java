package com.example.config;

import com.example.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    CustomAuthenticationProvider customAuthenticationProvider;

    @Autowired
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
//                .securityContext(context -> context
//                        .requireExplicitSave(true))  // 替代 eraseCredentials
                .sessionManagement(conf -> conf
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(customAuthenticationProvider)  // 配置认证提供者
                // 将自定义CORS过滤器添加到Security过滤器链的最前面

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                //对于登录请求我们不予以拦截
                .authorizeHttpRequests(conf -> conf
                        .requestMatchers(
                                "/user/register",
                                "/user/login",
                                "/auth/**",
                                "/captcha/**",
                                "/user/ask-code/**",     // 获取验证码接口
                                "/user/resetConfirm/**", // 验证验证码有效性接口
                                "/user/resetPassword/**" // 重置密码接口
                                , "/user/getAvatar/**"
                                ,"/file/ts/**"

                        ).permitAll()  // 多个路径
                        .anyRequest().authenticated()
                );


//                .requestMatchers("/auth/login", "/auth/register").permitAll()


        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}