package com.example.entity.Vo.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除文件请求VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteFileVo {
    /**
     * 文件ID列表，多个ID用逗号分隔
     */
    private String fileIds;
} 