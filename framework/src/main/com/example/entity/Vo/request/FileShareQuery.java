package com.example.entity.Vo.request;

import lombok.Data;

/**
 * 分享信息参数
 */
@Data
public class FileShareQuery  {


    /**
     * 分享ID
     */
    private String shareId;

    private String shareIdFuzzy;

    /**
     * 文件ID
     */
    private String fileId;

    private String fileIdFuzzy;

    /**
     * 用户ID
     */
    private Integer userId;

    private String userIdFuzzy;

    /**
     * 有效期类型 0:1天 1:7天 2:30天 3:永久有效
     */
    private Integer validType;

    /**
     * 失效时间
     */
    private String expireTime;

    private String expireTimeStart;

    private String expireTimeEnd;

    /**
     * 分享时间
     */
    private String shareTime;

    private String shareTimeStart;

    private String shareTimeEnd;

    /**
     * 提取码
     */
    private String code;

    private String codeFuzzy;

    /**
     * 浏览次数
     */
    private Integer showCount;

    private Boolean queryFileName;


    private Integer pageNo;
    private Integer pageSize;
    private String orderBy;


}
