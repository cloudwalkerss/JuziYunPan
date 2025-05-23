package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.Dto.FileInfo;
import com.example.entity.Dto.UploadResultDto;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.FileInfoQuery;
import com.example.entity.Vo.request.createFolderVo;
import com.example.entity.Vo.request.reNameVo;
import com.example.entity.Vo.request.FileUploadVO;
import com.example.entity.Vo.response.FileInfoVO;
import com.example.entity.Vo.response.PaginationResultVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * 文件服务接口
 */
public interface FileService extends IService<FileInfo> {
    
    /**
     * 分页查询文件列表
     * @param query 查询参数
     * @return 分页结果
     */
    PaginationResultVO<FileInfoVO> findListByPage(FileInfoQuery query);

    /**
     * 上传文件（使用VO对象）
     * @param userId 用户ID
     * @param uploadVO 文件上传VO对象
     * @return 上传结果
     * @throws IOException IO异常
     */
    UploadResultDto uploadFile(Integer userId, FileUploadVO uploadVO) throws IOException;

    public  void getFIle(HttpServletRequest request,HttpServletResponse response, String fileId,Integer userId) throws IOException;

    RestBean newFolder(Integer userId, createFolderVo vo);

    List<FileInfo> findListByParam(FileInfoQuery infoQuery);


    RestBean rename( Integer userId, reNameVo vo);

    RestBean changeFileFolder(String fileIds, String filePid, Integer userId);

    RestBean createDownloadUrl(Integer userId, String fileId);

    void download(HttpServletRequest request, HttpServletResponse response, String code) throws UnsupportedEncodingException;

    RestBean removeFile2RecycleBatch(Integer userId, String fileIds);

    RestBean recoverFileBatch(Integer userId, String fileIds);

    RestBean delFileBatch(Integer userId, String fileIds, boolean adminOp);

     RestBean getFolderInfo(String path,Integer userId);

    FileInfo getFileInfoByFileIdAndUserId(String fileId, Integer userId);

    void checkRootFilePid(String fileId, Integer shareUserId, String filePid);



    @Transactional
    void saveShare(String shareRootFilePid, String shareFileIds, String myFolderId, Integer shareUserId, Integer cureentUserId);

    UploadResultDto uploadFile(Integer userId, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex,
                               Integer chunks) throws IOException;
}
