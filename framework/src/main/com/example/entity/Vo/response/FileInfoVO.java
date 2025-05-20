package com.example.entity.Vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 文件信息展示类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileInfoVO {
    
    /**
     * 文件ID
     */
    private String fileId;
    
    /**
     * 用户ID
     */
    private Integer userId;
    
    /**
     * 用户昵称
     */
    private String nickName;
    
    /**
     * 父级ID
     */
    private String filePid;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 文件名称
     */
    private String fileName;
    
    /**
     * 封面
     */
    private String fileCover;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;
    
    /**
     * 0:文件 1:目录
     */
    private Integer folderType;
    
    /**
     * 1:视频 2:音频  3:图片 4:文档 5:其他
     */
    private Integer fileCategory;
    
    /**
     * 1:视频 2:音频  3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他
     */
    private Integer fileType;
    
    /**
     * 0:转码中 1转码失败 2:转码成功
     */
    private Integer status;
    
    /**
     * 删除标记 0:删除  1:回收站  2:正常
     */
    private Integer delFlag;
} 