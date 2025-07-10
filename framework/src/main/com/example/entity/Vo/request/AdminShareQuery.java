package com.example.entity.Vo.request;

import lombok.Data;

/**
 * 管理员分享查询参数
 */
@Data
public class AdminShareQuery {
    
    /**
     * 文件名关键字
     */
    private String keyword;
    
    /**
     * 用户ID
     */
    private Integer userId;
    
    /**
     * 有效期类型 0:1天 1:7天 2:30天 3:永久有效
     */
    private Integer validType;
    
    /**
     * 状态 0:有效 1:已过期
     */
    private Integer status;
    
    /**
     * 当前页码
     */
    private Integer pageNo;
    
    /**
     * 每页条数
     */
    private Integer pageSize;
    
    /**
     * 排序字段
     */
    private String orderBy;
}