package com.example.entity.Vo.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加载文件夹请求VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoadFolderVo {
    /**
     * 父文件夹ID
     */
    private String filePId;
    
    /**
     * 排除的文件ID列表，多个ID用逗号分隔
     */
    private String excludeFileIds;
} 