package com.example.entity.Vo.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class createFolderVo {
    private String fileName;
    private  String filePid;

}
