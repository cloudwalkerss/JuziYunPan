package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.Dto.FileShare;
import com.example.entity.Vo.request.FileShareQuery;
import com.example.entity.Vo.response.PaginationResultVO;
import org.springframework.stereotype.Service;


public interface FileShareService extends IService<FileShare> {
    PaginationResultVO findListByPage(FileShareQuery query);

    void saveShare(FileShare share);

    void deleteFileShareBatch(String[] split, Integer userId);

    FileShare getFileShareByShareId(String shareId);

    void updateShareCount(String shareId);
}
