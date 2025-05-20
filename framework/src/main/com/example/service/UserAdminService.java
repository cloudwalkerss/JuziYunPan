package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.Dto.Account;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.UserAdminQuery;
import jakarta.servlet.http.HttpServletResponse;


public interface UserAdminService extends IService<Account> {

    RestBean loadUser(UserAdminQuery userAdminQuery);

    void updateUserStatus(Integer userId, Integer status);

    void changeUserSpace(Integer userId, Integer changeSpace);

    void getfile(HttpServletResponse response, String fileId, String userId);
}
