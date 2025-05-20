package com.example.entity.Dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

public class UploadResultDto implements Serializable {
    @Getter
    @Setter
    private String fileId;
    @Getter
    @Setter
    private String status;


}