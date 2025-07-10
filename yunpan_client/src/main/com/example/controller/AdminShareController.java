package com.example.controller;

import com.example.component.RedisComponent;
import com.example.constants.Const;
import com.example.entity.Dto.FileShare;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.AdminShareQuery;
import com.example.entity.Vo.response.PaginationResultVO;
import com.example.entity.Vo.response.ShareInfoVO;
import com.example.service.FileShareService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员分享管理控制器
 */
@RestController("adminShareController")
@RequestMapping("/admin/share")
public class AdminShareController {

    @Autowired
    private RedisComponent redisComponent;

    @Resource
    private FileShareService fileShareService;

    /**
     * 管理员查询所有用户的分享
     * @param adminShareQuery 查询参数
     * @return 分页数据
     */
    @RequestMapping("/list")
    public RestBean list(@RequestBody AdminShareQuery adminShareQuery) {
        // 验证是否为管理员的逻辑已在拦截器或过滤器中处理
        PaginationResultVO<FileShare> result = fileShareService.findShareListForAdmin(adminShareQuery);
        return RestBean.success(result);
    }

    /**
     * 管理员取消分享
     * @param shareInfo 分享ID信息
     * @return 处理结果
     */
    @RequestMapping("/cancel")
    public RestBean cancelShare(@RequestBody ShareInfo shareInfo) {
        // 管理员取消分享，不需要验证用户ID
        fileShareService.deleteFileShareByAdmin(shareInfo.getShareId());
        return RestBean.success();
    }

    /**
     * 管理员查看分享详情
     * @param shareId 分享ID
     * @return 分享详情
     */
    @RequestMapping("/detail")
    public RestBean getShareDetail(String shareId) {
        ShareInfoVO shareInfoVO = fileShareService.getShareDetailForAdmin(shareId);
        return RestBean.success(shareInfoVO);
    }

    /**
     * 接收分享ID的内部类
     */
    static class ShareInfo {
        private String shareId;

        public String getShareId() {
            return shareId;
        }

        public void setShareId(String shareId) {
            this.shareId = shareId;
        }
    }
} 