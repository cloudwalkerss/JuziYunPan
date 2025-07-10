package com.example.entity.Vo.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
@Data
public class ShareInfoVO {
    private String shareId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date shareTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;
    private String nickName;
    private String fileName;
    private Boolean currentUser;
    private String fileId;
    private String avatar;
    private Integer fileType;
    private Integer userId;
    /**
     * 文件大小
     */
    private Long fileSize;
    /**
     * 封面
     */
    private String fileCover;

}
