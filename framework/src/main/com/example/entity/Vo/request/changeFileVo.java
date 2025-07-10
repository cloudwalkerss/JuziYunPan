package com.example.entity.Vo.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class changeFileVo {
    private String fileIds;
    private String filePid;
   private Integer userId;
}
