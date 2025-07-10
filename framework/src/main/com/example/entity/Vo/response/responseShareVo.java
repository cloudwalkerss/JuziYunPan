package com.example.entity.Vo.response;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.entity.Enum.DateTimePatternEnum;
import com.example.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@TableName("file_share")
public class responseShareVo {

    /**
     * 分享ID
     */
    private String shareId;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 有效期类型 0:1天 1:7天 2:30天 3:永久有效
     */
    private Integer validType;

    /**
     * 失效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /**
     * 分享时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date shareTime;

    /**
     * 提取码
     */
    private String code;

    /**
     * 浏览次数
     */
    private Integer showCount;

    @TableField(exist = false)
    private String fileName;

    /**
     * 0:文件 1:目录
     */
    @TableField(exist = false)
    private Integer folderType;

    /**
     * 1:视频 2:音频  3:图片 4:文档 5:其他
     */
    @TableField(exist = false)
    private Integer fileCategory;

    /**
     * 1:视频 2:音频  3:图片 4:pdf 5:doc 6:excel 7:txt 8:code 9:zip 10:其他
     */
    @TableField(exist = false)
    private Integer fileType;

    /**
     * 封面
     */
    @TableField(exist = false)
    private String fileCover;


    @Override
    public String toString() {
        return "分享ID:" + (shareId == null ? "空" : shareId) + "，文件ID:" + (fileId == null ? "空" : fileId) + "，用户ID:" + (userId == null ? "空" : userId) + "，有效期类型 0:1天 " +
                "1:7天 2:30天 3:永久有效:" + (validType == null ? "空" : validType) + "，失效时间:" + (expireTime == null ? "空" : DateUtil.format(expireTime,
                DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + "，分享时间:" + (shareTime == null ? "空" : DateUtil.format(shareTime,
                DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + "，提取码:" + (code == null ? "空" : code) + "，浏览次数:" + (showCount == null ? "空" : showCount);
    }

}
