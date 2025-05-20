package com.example.entity.Vo.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传请求参数VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadVO {
    /**
     * 文件ID，可选，用于断点续传
     */
    private String fileId;
    
    /**
     * 上传的文件
     */
    private MultipartFile file;
    
    /**
     * 文件名称
     */
    private String fileName;
    
    /**
     * 父文件夹ID
     */
    private String filePid;
    
    /**
     * 文件MD5值，用于文件秒传
     */
    private String fileMd5;
    
    /**
     * 当前分片索引
     */
    private Integer chunkIndex;
    
    /**
     * 总分片数
     */
    private Integer chunks;
} 