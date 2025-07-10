package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.Dto.FileShare;
import com.example.entity.Vo.request.AdminShareQuery;
import com.example.entity.Vo.request.FileShareQuery;
import com.example.entity.Vo.request.loadShareVo;
import com.example.entity.Vo.response.PaginationResultVO;
import com.example.entity.Vo.response.ShareInfoVO;
import org.springframework.stereotype.Service;


public interface FileShareService extends IService<FileShare> {
    PaginationResultVO findListByPage(loadShareVo vo);

    void saveShare(FileShare share);

    void deleteFileShareBatch(String[] split, Integer userId);

    FileShare getFileShareByShareId(String shareId);

    void updateShareCount(String shareId);
    
    /**
     * 管理员查询所有用户的分享列表
     * @param query 查询参数
     * @return 分页结果
     */
    PaginationResultVO<FileShare> findShareListForAdmin(AdminShareQuery query);
    
    /**
     * 管理员删除分享（不需要验证用户ID）
     * @param shareId 分享ID
     */
    void deleteFileShareByAdmin(String shareId);
    
    /**
     * 管理员获取分享详情
     * @param shareId 分享ID
     * @return 分享详情
     */
    ShareInfoVO getShareDetailForAdmin(String shareId);
}
