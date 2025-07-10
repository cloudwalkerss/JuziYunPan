package com.example.entity.Vo.request;

import lombok.Data;

@Data
public class loadShareVo {
    private Integer userId;
    private  Integer pageNo;
    private  Integer pageSize;
}
