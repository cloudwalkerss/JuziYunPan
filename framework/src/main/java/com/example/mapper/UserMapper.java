package com.example.mapper;

import com.example.entity.Dto.Account;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    @Select("select * from account where username =#{username}")
    Account findByUsername(String username);

    @Insert("insert into account (username, email, password, register_time, update_time, role, avatar, nick_name)" +
            "values (#{account.username}, #{account.email}, #{account.password}, #{account.registerTime}, #{account.updateTime}, " +
            "#{account.role}, #{account.avatar}, #{account.nickname})")
    void createAccount(@Param("account") Account account);

    @Select("select * from account where email=#{email}")
    Account findByEmail(String email);
    @Select("select * from account where id=#{userId}")
    Account findById(Integer userId);

     @Update("update  account  set  password =#{password} where email=#{email} ")
    void updatePassword(@Param("email") String email,@Param("password") String password);
}
