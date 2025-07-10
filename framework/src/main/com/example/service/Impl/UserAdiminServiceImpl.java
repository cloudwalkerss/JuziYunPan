package com.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.component.RedisComponent;
import com.example.constants.Constants;
import com.example.entity.Dto.Account;
import com.example.entity.Enum.UserStatusEnum;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.UserAdminQuery;
import com.example.entity.Vo.response.PaginationResultVO;
import com.example.entity.Vo.response.UserAdminV0;
import com.example.mapper.UserMapper;
import com.example.service.UserAdminService;
import com.example.utils.BeanCopyUtils;
import io.lettuce.core.support.caching.CacheAccessor;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAdiminServiceImpl extends ServiceImpl<UserMapper, Account> implements UserAdminService {

@Autowired
    private  UserMapper userMapper;
    @Autowired
    BeanCopyUtils beanCopyUtils;
@Autowired
    RedisComponent redisComponent;


    @Override
    public RestBean loadUser(UserAdminQuery query) {

        // 根据请求参数然后查询，然后分页返回
        Account account = new Account();
        Account copiedBean = beanCopyUtils.copyBean(query, account.getClass());

        // 创建分页对象
        int pageNo = query.getPageNo() != null ? query.getPageNo() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 10;

        Page<Account> page = new Page<>(pageNo, pageSize);

        // 创建查询条件
        QueryWrapper<Account> queryWrapper = new QueryWrapper<>(copiedBean);


            // 默认按注册时间降序排序
            queryWrapper.orderByDesc("register_time");

        // 执行分页查询
        Page<Account> pageResult = baseMapper.selectPage(page, queryWrapper);

        // 获取结果列表
        List<Account> accounts = pageResult.getRecords();

        // 转换为VO对象
        List<UserAdminV0> userAdminVos = beanCopyUtils.copyBeanList(accounts, UserAdminV0.class);

        // 构建分页结果对象
        PaginationResultVO<UserAdminV0> result = new PaginationResultVO<>(
                (int) pageResult.getTotal(),        // 总记录数
                pageSize,                           // 每页大小
                pageNo,                             // 当前页码
                (int) pageResult.getPages(),        // 总页数
                userAdminVos                        // 数据列表
        );

        // 返回响应对象
        return RestBean.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Integer userId, Integer status) {

              UpdateWrapper<Account> updateWrapper = new UpdateWrapper<>();
              updateWrapper.eq("id", userId)
                      .set("Banned", status);
               baseMapper.update(null, updateWrapper);

    }

    @Override
    public void changeUserSpace(Integer userId, Integer changeSpace) {
         Long space =changeSpace* Constants.MB;
         this.userMapper.updateUserSpace(userId,null,space);
         redisComponent.resetUserSpaceUse(userId);

    }

    @Override
    public void getfile(HttpServletResponse response, String fileId, String userId) {

    }


}
