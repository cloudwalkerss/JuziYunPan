package com.example.entity.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DownloadFileDto {
    private String downloadCode;
    private String fileId;
    private String fileName;
    private String filePath;

}
