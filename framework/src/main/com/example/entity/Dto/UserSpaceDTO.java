package com.example.entity.Dto;

import lombok.Data;

/**
 * 用户空间使用情况DTO
 */
@Data
public class UserSpaceDTO {
    /**
     * 用户ID
     */
    private Integer id;
    
    /**
     * 已使用空间（单位：字节）
     */
    private Long useSpace;
    
    /**
     * 总空间（单位：字节）
     */
    private Long totalSpace;
} 