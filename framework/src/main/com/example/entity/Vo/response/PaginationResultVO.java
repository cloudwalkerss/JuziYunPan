package com.example.entity.Vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页查询结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginationResultVO<T> {
    /**
     * 总记录数
     */
    private Integer totalCount;
    
    /**
     * 每页记录数
     */
    private Integer pageSize;
    
    /**
     * 当前页码
     */
    private Integer pageNo;
    
    /**
     * 总页码
     */
    private Integer pageTotal;
    
    /**
     * 分页数据
     */
    private List<T> list;
} 