package com.example.service.Impl;


import com.alibaba.fastjson2.util.DateUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.component.RedisComponent;
import com.example.constants.AppConfig;
import com.example.constants.Constants;
import com.example.entity.Dto.*;
import com.example.entity.Enum.*;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.FileInfoQuery;
import com.example.entity.Vo.request.FileUploadVO;
import com.example.entity.Vo.request.createFolderVo;
import com.example.entity.Vo.request.reNameVo;
import com.example.entity.Vo.response.FileInfoVO;
import com.example.entity.Vo.response.FolderVO;
import com.example.entity.Vo.response.PaginationResultVO;
import com.example.exception.BusinessException;
import com.example.mapper.FileMapper;
import com.example.mapper.UserMapper;
import com.example.service.FileService;

import com.example.utils.*;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.micrometer.common.util.internal.logging.InternalLogger;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.service.Impl.MailService.logger;


/**
 * 文件服务实现类
 */
@Service
@Slf4j
public class FileServiceImpl extends ServiceImpl<FileMapper, FileInfo> implements FileService {

    @Autowired
    private FileMapper fileMapper;
     @Resource
    StringTools stringTools;
    @Autowired
    private RedisComponent redisComponent;
    @Autowired
    private UserMapper userMapper;
    @Autowired
     private AppConfig appConfig;
    @Resource
    @Lazy
    private FileServiceImpl fileInfoService;
    @Autowired
    private BeanCopyUtils beanCopyUtils;

    @Override
    public FileMapper getBaseMapper() {
        return super.getBaseMapper();
    }

    /**
     * 分页查询文件列表
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PaginationResultVO<FileInfoVO> findListByPage(FileInfoQuery query) {
        // 设置查询条件
//        FileInfoQuery fileInfo = new FileInfoQuery();
//        fileInfo.setUserId(query.getUserId());
//        fileInfo.setFileCategory(query.getFileCategory());
//        fileInfo.setDelFlag(query.getDelFlag());
        
        // 使用PageHelper进行分页，这是关键步骤
        // startPage方法会自动拦截接下来的查询，并添加分页条件
        PageHelper.startPage(query.getPageNo(), query.getPageSize());
        
        // 如果有排序要求，设置排序
        if (query.getOrderBy() != null) {
            PageHelper.orderBy(query.getOrderBy());
        }
        // 先获取分页信息

        // 执行查询，PageHelper会自动处理分页
        List<FileInfo> list = fileMapper.selectFileList(query);

        PageInfo<FileInfo> pageInfo = new PageInfo<>(list);

        List<FileInfoVO> fileInfoVOS = beanCopyUtils.copyBeanList(list, FileInfoVO.class);
        
//        // 使用PageInfo包装查询结果，获取分页相关信息
//        PageInfo<FileInfoVO> pageInfo = new PageInfo<>(fileInfoVOS);
        
        // 构建并返回分页结果
        return new PaginationResultVO<>(
                (int) pageInfo.getTotal(),  // 总记录数
                query.getPageSize(),        // 每页大小
                query.getPageNo(),          // 当前页码
                pageInfo.getPages(),        // 总页数
                fileInfoVOS                       // 当前页数据
        );
    }
  @Transactional(rollbackFor = Exception.class)
    @Override
    public UploadResultDto uploadFile(Integer userId, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks) throws IOException {
        File tempFileFolder = null;
        Boolean uploadSuccess = true;
        try {
            UploadResultDto resultDto = new UploadResultDto();
            if (StringTools.isEmpty(fileId)) {
                fileId = StringTools.getRandomString(Constants.LENGTH_10);
            }
            resultDto.setFileId(fileId);
        Date curDate = new Date();
            
            // 获取用户空间使用信息
            UserSpaceDTO spaceDto = redisComponent.getUserSpaceUse(userId);
            
            // 处理秒传
            if (chunkIndex == 0) {
                FileInfoQuery infoQuery = new FileInfoQuery();
                infoQuery.setFileMd5(fileMd5);
                infoQuery.setStatus(FileStatusEnums.USING.getStatus());
                infoQuery.setFolderType(FileFolderTypeEnums.FILE.getType());
                List<FileInfo> dbFileList = fileMapper.selectFileList(infoQuery);
                
            if (!dbFileList.isEmpty()) {
                FileInfo dbFile = dbFileList.get(0);
                    // 判断空间是否足够
                    if (dbFile.getFileSize() + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                        throw new BusinessException(ResponseCodeEnum.CODE_904);
                    }
                    
                    // 复制文件信息
                    dbFile.setFileId(fileId);
                    dbFile.setFilePid(filePid);
                    dbFile.setUserId(userId);
                    dbFile.setCreateTime(curDate);
                    dbFile.setUpdateTime(curDate);
                    dbFile.setStatus(FileStatusEnums.USING.getStatus());
                    dbFile.setDelFlag(FileDelFlagEnums.USING.getFlag());
                    dbFile.setFileMd5(fileMd5);
                    
                    // 自动重命名
                    fileName = autoRename(filePid, userId, fileName);
                    dbFile.setFileName(fileName);
                    
                    fileMapper.insert(dbFile);
                    resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());

                    // 更新用户空间使用
                    updateUserSpace(userId, dbFile.getFileSize());

                    return resultDto;
            }
        }

        // 获取文件大小，处理可能为负数的情况
        long fileSize = file.getSize();
        if (fileSize < 0) {
            try {
                fileSize = file.getInputStream().available();
                log.info("文件大小为负，从输入流获取大小: {}", fileSize);
            } catch (Exception e) {
                log.error("无法获取文件大小", e);
                fileSize = 0; // 如果无法获取，设置为0以避免空间判断出错
            }
        }

            // 创建临时目录
        String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
        String currentUserFolderName = userId + fileId;
            tempFileFolder = new File(tempFolderName + currentUserFolderName);
            if (!tempFileFolder.exists()) {
                tempFileFolder.mkdirs();
            }
            
            // 判断用户空间是否足够
            Long currentTempSize = redisComponent.getFileTempSize(userId, fileId);
            if (fileSize + currentTempSize + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                throw new BusinessException(ResponseCodeEnum.CODE_904);
            }
            
            // 保存分片文件
            File newFile = new File(tempFileFolder.getPath() + "/" + chunkIndex);
            file.transferTo(newFile);
            redisComponent.saveFileTempSize(userId, fileId, fileSize);
            
            // 如果不是最后一个分片，返回上传中状态
            if (chunkIndex < chunks - 1) {
                resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
                return resultDto;
            }
            
            // 处理最后一个分片
            String month = DateUtils.format(curDate, DateTimePatternEnum.YYYYMM.getPattern());
            String fileSuffix = StringTools.getFileSuffix(fileName);
            String realFileName = currentUserFolderName + fileSuffix;
            FileTypeEnums fileType = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            
            // 自动重命名
            fileName = autoRename(filePid, userId, fileName);
            
            // 创建文件信息
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setUserId(userId);
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFileName(fileName);
            fileInfo.setFilePath(month + "/" + realFileName);
            fileInfo.setFilePid(filePid);
            fileInfo.setCreateTime(curDate);
            fileInfo.setUpdateTime(curDate);
            fileInfo.setFileCategory(fileType.getCategory().getCategory());
            fileInfo.setFileType(fileType.getType());
            fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
            fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());

            // 设置文件大小，避免空指针异常
            Long totalSize = redisComponent.getFileTempSize(userId, fileId);
            if (totalSize <= 0) {
                totalSize = fileSize * chunks; // 估算总大小
                log.info("从Redis获取的文件大小无效，使用估算值: {}", totalSize);
            }
            fileInfo.setFileSize(totalSize);

            fileMapper.insert(fileInfo);
            
            // 更新用户空间使用
            updateUserSpace(userId, totalSize);

            resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
            
            // 事务提交后进行异步文件转码
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileInfoService.transferFile(fileInfo.getFileId(), userId);
                }
            });

            return resultDto;
            
        } catch (BusinessException e) {
            uploadSuccess = false;
            log.error("文件上传失败", e);
            throw e;
        } catch (Exception e) {
            uploadSuccess = false;
            log.error("文件上传失败", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500.getMsg() + ":" + e.getMessage());
        } finally {
            if (!uploadSuccess && tempFileFolder != null) {
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                } catch (IOException e) {
                    log.error("删除临时目录失败", e);
                }
            }
        }
    }
     //打开文件
    @Override
    public void getFIle(HttpServletRequest request,HttpServletResponse response, String fileId, Integer userId) throws IOException {
        String filePath = null;
        if (fileId.endsWith(".ts")) {
            String[] tsArray = fileId.split("_");
            String realFileId = tsArray[0];
            FileInfo fileInfo = fileMapper.selectByFileIdAndUserId(realFileId, userId);
            if (fileInfo == null) {
                return;
            }
            String fileName = fileInfo.getFilePath();
            fileName = StringTools.getFileNameNoSuffix(fileName) + "/" + fileId;
            filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileName;
            readFile(response, null, filePath);
        } else {
            FileInfo fileInfo = fileMapper.selectByFileIdAndUserId(fileId, userId);
            if (fileInfo == null) {
                return;
            }
            //判断一下文件类型
            if (FileCategoryEnums.VIDEO.getCategory().equals(fileInfo.getFileCategory())) {
                String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());//获得无后缀的地址
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileNameNoSuffix + "/" + Constants.M3U8_NAME;
//                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath();
                File file = new File(filePath);
                if (!file.exists()) {
                    return;
                }
                readFile(response, filePath);
            } else {
                //直接读
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath();
                readFile(response, request, filePath);
            }
        }
    }
   //创建新文件
    @Override
    public RestBean newFolder( Integer userId, createFolderVo vo) {
          if(StringTools.isEmpty(vo.getFileName())||StringTools.isEmpty(vo.getFilePid())){
            return RestBean.failure(405,"参数错误");
          }
           String filePid=vo.getFilePid();
           String fileName=vo.getFileName();
         //可能文件名重复要判断
        Boolean is=  checkFileName(filePid,userId,fileName,FileFolderTypeEnums.FOLDER.getType());
        if(!is) return RestBean.failure(405,"该文件夹下已有同名文件夹");
          Date curDate=new Date();

        FileInfo dbInfo = new FileInfo();
        dbInfo.setFileId(StringTools.getRandomString(Constants.LENGTH_10));
        dbInfo.setUserId(userId);
        dbInfo.setFileName(fileName);
        dbInfo.setFilePid(filePid);
        dbInfo.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        dbInfo.setUpdateTime(curDate);
        dbInfo.setCreateTime(curDate);
        dbInfo.setStatus(FileStatusEnums.USING.getStatus());
        dbInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        this.fileMapper.insert(dbInfo);
        return RestBean.success(beanCopyUtils.copyBean(dbInfo,FileInfoVO.class));


    }

    @Override
    public List<FileInfo> findListByParam(FileInfoQuery param) {
        // 创建查询条件
        QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();

        // 根据参数动态构建查询条件
        if (param != null) {
            // 文件ID条件
            if (param.getFileId() != null) {
                queryWrapper.eq("file_id", param.getFileId());
            }
            
            // 文件ID数组条件
            if (param.getFileIdArray() != null && param.getFileIdArray().length > 0) {
                queryWrapper.in("file_id", Arrays.asList(param.getFileIdArray()));
            }
            
            // 排除文件ID数组条件
            if (param.getExcludeFileIdArray() != null && param.getExcludeFileIdArray().length > 0) {
                queryWrapper.notIn("file_id", Arrays.asList(param.getExcludeFileIdArray()));
            }

            // 用户ID条件
            if (param.getUserId() != null) {
                queryWrapper.eq("user_id", param.getUserId());
            }

            // 文件名称条件，支持模糊查询
            if (!StringUtils.isEmpty(param.getFileName())) {
                queryWrapper.like("file_name", param.getFileName());
            }

            // 文件夹类型条件
            if (param.getFolderType() != null) {
                queryWrapper.eq("folder_type", param.getFolderType());
            }

            // 父级目录ID条件
            if (!StringUtils.isEmpty(param.getFilePid())) {
                queryWrapper.eq("file_pid", param.getFilePid());
            }

            // 删除标志条件
            if (param.getDelFlag() != null) {
                queryWrapper.eq("del_flag", param.getDelFlag());
            }

            // 文件类别条件
            if (param.getFileCategory() != null) {
                queryWrapper.eq("file_category", param.getFileCategory());
            }

            // 文件类型条件
            if (param.getFileType() != null) {
                queryWrapper.eq("file_type", param.getFileType());
            }

            // 设置排序
            if (!StringUtils.isEmpty(param.getOrderBy())) {
                queryWrapper.last("ORDER BY " + param.getOrderBy());
            } else {
                // 默认按创建时间排序
                queryWrapper.orderByDesc("create_time");
            }
        }

        // 执行查询并返回结果
        return fileMapper.selectList(queryWrapper);
    }
   //重命名文件
    @Transactional(rollbackFor = Exception.class)
    @Override
    public RestBean rename(Integer userId, reNameVo vo) {
         String fileName=vo.getFileName();
         String fileId=vo.getFileId();
         FileInfo fileInfo=fileMapper.selectByFileIdAndUserId(fileId,userId);
         if(fileInfo==null){
             return RestBean.failure(600, "文件不存在");
         }
       Boolean is=  checkFileName(fileInfo.getFilePid(),userId,fileName,fileInfo.getFolderType());
         if(is==false) return RestBean.failure(405,"此目录已经存在同名文件");

         //获取文件后缀
          if(FileFolderTypeEnums.FILE.getType().equals(fileInfo.getFolderType())){
              fileName=fileName+StringTools.getFileSuffix(fileInfo.getFileName());

          }
          Date curDate=new Date();
          FileInfo dbInfo=new FileInfo();
          dbInfo.setFileId(fileId);
          dbInfo.setFileName(fileName);
          dbInfo.setUpdateTime(curDate);

        try {
            this.fileMapper.updateById(dbInfo);
    
            QueryWrapper<FileInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("file_pid", fileInfo.getFilePid())
                    .eq("user_id", userId)
                    .eq("file_name", fileName)
                    .eq("folder_type", fileInfo.getFolderType())
                    .eq("del_flag", FileDelFlagEnums.USING.getFlag());
    
            Integer count = Math.toIntExact(fileMapper.selectCount(wrapper));
            if (count > 1) {
                return RestBean.failure(405,"此目录已经存在同名文件");
            }
    
            return RestBean.success(fileName);
        } catch (Exception e) {
            log.error("重命名文件时发生错误: {}", e.getMessage(), e);
            return RestBean.failure(500, "重命名文件失败: " + e.getMessage());
        }
    }
    /**
     * 检查目标文件夹是否是源文件夹的子文件夹（递归检查）
     * @param parentFolderId 源文件夹ID
     * @param targetFolderId 目标文件夹ID
     * @param userId 用户ID
     * @return 如果目标是源的子文件夹则返回true，否则返回false
     */
    private boolean isSubFolder(String parentFolderId, String targetFolderId, Integer userId) {
        // 如果IDs相等，说明是移动到自身
        if (parentFolderId.equals(targetFolderId)) {
            return true;
        }
        
        // 获取目标文件夹信息
        FileInfo targetFolder = fileMapper.selectByFileIdAndUserId(targetFolderId, userId);
        
        // 如果目标文件夹不存在或是根目录，则不是子文件夹
        if (targetFolder == null || Constants.ZERO_STR.equals(targetFolder.getFilePid())) {
            return false;
        }
        
        // 递归检查目标文件夹的父文件夹
        return isSubFolder(parentFolderId, targetFolder.getFilePid(), userId);
    }

    //移动文件
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RestBean changeFileFolder(String fileIds, String filePid, Integer userId) {
        // 检查参数是否为空
        if (fileIds == null || fileIds.trim().isEmpty()) {
            return RestBean.failure(600, "参数错误");
        }
        if (filePid == null) {
            return RestBean.failure(600, "参数错误");
        }
        
        // 防止自我移动（文件夹不能移动到自身内部）
        if (fileIds.equals(filePid)) {
            return RestBean.failure(600, "不能将文件夹移动到自身内部");
        }

        // 验证目标文件夹是否存在（根目录除外，根目录永远存在）
        if (!Constants.ZERO_STR.equals(filePid)) {
            FileInfo fileInfo = fileMapper.selectByFileIdAndUserId(filePid, userId);
            if (fileInfo == null || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())) {
                return RestBean.failure(600, "目标文件夹不存在");
            }
        }
        
        try {
            // 拆分要移动的文件ID
            String[] fileIdArray = fileIds.split(",");
            
            // 检查每个要移动的文件夹，确保不会形成循环引用
            for (String fileId : fileIdArray) {
                FileInfo sourceFile = fileMapper.selectByFileIdAndUserId(fileId, userId);
                // 如果是文件夹，需要进行递归检查
                if (sourceFile != null && FileFolderTypeEnums.FOLDER.getType().equals(sourceFile.getFolderType())) {
                    // 递归检查目标文件夹是否是源文件夹的子文件夹
                    if (isSubFolder(fileId, filePid, userId)) {
                        return RestBean.failure(600, "不能将文件夹移动到它的子文件夹中");
                    }
                }
            }

            // 查询目标文件夹中已有的文件列表，用于检查命名冲突
            FileInfoQuery query = new FileInfoQuery();
            query.setFilePid(filePid);
            query.setUserId(userId);
            query.setDelFlag(FileDelFlagEnums.USING.getFlag());
            List<FileInfo> dbFileList = fileInfoService.findListByParam(query);

            // 创建文件名映射，便于快速查找是否有同名文件
            Map<String, FileInfo> dbFileNameMap = dbFileList.stream()
                .collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));
            
            // 查询要移动的文件/文件夹
            query = new FileInfoQuery();
            query.setUserId(userId);
            query.setFileIdArray(fileIdArray);
            query.setDelFlag(FileDelFlagEnums.USING.getFlag());
            List<FileInfo> selectFileList = fileInfoService.findListByParam(query);

            // 处理每个要移动的文件/文件夹
            for (FileInfo item : selectFileList) {
                // 检查是否有文件名冲突
                FileInfo existingFile = dbFileNameMap.get(item.getFileName());
                
                // 准备更新信息
                FileInfo updateInfo = new FileInfo();
                updateInfo.setFilePid(filePid);
                updateInfo.setUpdateTime(new Date());
                
                // 如果存在同名文件，则重命名
                if (existingFile != null) {
                    String newFileName = StringTools.rename(item.getFileName());
                    updateInfo.setFileName(newFileName);
                }
                
                // 使用MyBatisPlus更新数据库
                UpdateWrapper<FileInfo> wrapper = new UpdateWrapper<>();
                wrapper.eq("user_id", userId)
                      .eq("file_id", item.getFileId());
                
                fileMapper.update(updateInfo, wrapper);
            }
            
            return RestBean.success();
        } catch (Exception e) {
            log.error("移动文件时发生错误: {}", e.getMessage(), e);
            return RestBean.failure(500, "移动文件失败: " + e.getMessage());
        }
    }

    /**
     * 此方法已经不再使用。我们直接在changeFileFolder方法中移动文件，
     * 不需要额外处理子文件夹，因为在数据库中子文件已经通过parent_id关联到父文件夹，
     * 父文件夹移动后，子文件自然跟随父文件夹显示。
     */
    private void updateChildFileReferences(Integer userId, String folderId) {
        // 此方法无需实现
    }

    @Override
    public RestBean createDownloadUrl(Integer userId, String fileId) {
        FileInfo fileInfo = fileMapper.selectByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            return RestBean.failure(600, "文件不存在");
        }
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            return RestBean.failure(600, "不能下载文件夹");
        }
        
        try {
            String code = StringTools.getRandomString(Constants.LENGTH_50);
            DownloadFileDto downloadFileDto = new DownloadFileDto();
            downloadFileDto.setDownloadCode(code);
            downloadFileDto.setFilePath(fileInfo.getFilePath());
            downloadFileDto.setFileName(fileInfo.getFileName());
    
            redisComponent.saveDownloadCode(code, downloadFileDto);
    
            return RestBean.success(code);
        } catch (Exception e) {
            log.error("创建下载链接失败: {}", e.getMessage(), e);
            return RestBean.failure(500, "创建下载链接失败: " + e.getMessage());
        }
    }

    @Override
       public void download(HttpServletRequest request, HttpServletResponse response, String code) throws UnsupportedEncodingException {
            DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
            if (null == downloadFileDto) {
                return;
            }
            String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
            String fileName = downloadFileDto.getFileName();
            response.setContentType("application/x-msdownload; charset=UTF-8");
            if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {//IE浏览器
                fileName = URLEncoder.encode(fileName, "UTF-8");
            } else {
                fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
            }
            response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        readFile(response, request, filePath);
        }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RestBean removeFile2RecycleBatch(Integer userId, String fileIds) {
        // 检查文件ID是否为空
        if (fileIds == null || fileIds.trim().isEmpty()) {
            return RestBean.failure(600, "参数错误");
        }
        
        try {
            String[] fileIdArray = fileIds.split(",");
            List<String> fileIdList = Arrays.asList(fileIdArray);
    
            // 创建QueryWrapper
            QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId)
                    .eq("del_flag", FileDelFlagEnums.USING.getFlag())
                    .in("file_id", fileIdList);
    
            // 执行查询
            List<FileInfo> fileInfoList = fileMapper.selectList(queryWrapper);//用户选择的
    
            if (fileInfoList.isEmpty()) {
                return RestBean.failure(600, "文件不存在");
            }
    
            List<String> delFilePidList = new ArrayList<>();//所有的
    
            for (FileInfo fileInfo : fileInfoList) {//删除子目录里的所有文件，要递归，直到没有目录为止
                findAllSubFolderFileIdList(delFilePidList, userId, fileInfo.getFileId(), FileDelFlagEnums.USING.getFlag());
            }
            //将目录下的所有文件更新为已删除
            if (!delFilePidList.isEmpty()) {
                    //将所有都设置为删除
                UpdateWrapper<FileInfo> updateWrapper = new UpdateWrapper<>();
                updateWrapper.in("file_id", delFilePidList);
                updateWrapper.eq("user_id", userId);
                updateWrapper.set("del_flag", FileDelFlagEnums.DEL.getFlag());
                fileMapper.update(null, updateWrapper);
            }
    
            //将选中的文件更新为回收站
            List<String> delFileIdList = Arrays.asList(fileIdArray);
            QueryWrapper<FileInfo> wrapper = new QueryWrapper<>();
            wrapper.in("file_id", delFileIdList);
            wrapper.eq("user_id", userId);
             FileInfo updateInfo = new FileInfo();
             updateInfo.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
             updateInfo.setUpdateTime(new Date());
             fileMapper.update(updateInfo, wrapper);
             return RestBean.success();
        } catch (Exception e) {
            log.error("删除文件失败: {}", e.getMessage(), e);
            return RestBean.failure(500, "删除文件失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RestBean recoverFileBatch(Integer userId, String fileIds) {
        // 检查文件ID是否为空
        if (fileIds == null || fileIds.trim().isEmpty()) {
            return RestBean.failure(600, "参数错误");
        }
        
        try {
            String[] fileIdArray = fileIds.split(",");
    
            //找到所选择的文件
            QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId)
                    .eq("del_flag", FileDelFlagEnums.RECYCLE.getFlag())
                    .in("file_id", fileIdArray);
            List<FileInfo> fileInfoList = fileMapper.selectList(queryWrapper);
            if (fileInfoList.isEmpty()) {
                return RestBean.failure(600, "文件不存在");
            }
            //最终要将选中的文件包括目录里面的文件全部修改状态为恢复
            //需要递归恢复，跟递归删除一个道理
            List<String> delFilePidList = new ArrayList<>();
            for (FileInfo fileInfo : fileInfoList) {
                if(FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
                    findAllSubFolderFileIdList(delFilePidList, userId, fileInfo.getFileId(), FileDelFlagEnums.DEL.getFlag());
                }
            }
            //查询所有根目录的文件
            QueryWrapper<FileInfo> wrapper = new QueryWrapper<>();
            wrapper.eq("file_pid", Constants.ZERO_STR)
                    .eq("user_id", userId)
                    .eq("del_flag", FileDelFlagEnums.USING.getFlag());
            List<FileInfo>rootFileInfoList = fileMapper.selectList(wrapper);
    
            Map<String, FileInfo> rootMap = rootFileInfoList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));
            if(!delFilePidList.isEmpty()) {
                UpdateWrapper<FileInfo> updateWrapper = new UpdateWrapper<>();
                updateWrapper.in("file_id", delFilePidList)
                        .eq("user_id", userId)
                        .set("del_flag", FileDelFlagEnums.USING.getFlag());
                //选中的目录下面的文件目录全删除
                fileMapper.update(null, updateWrapper);
            }
            UpdateWrapper<FileInfo> updateWrapper = new UpdateWrapper<>();
            updateWrapper.in("file_id", fileIdArray)
                        .eq("user_id", userId)
                        .eq("del_flag", FileDelFlagEnums.RECYCLE.getFlag())
                        .set("del_flag", FileDelFlagEnums.USING.getFlag())
                        .set("file_pid", Constants.ZERO_STR)
                        .set("update_time", new Date());
    
            fileMapper.update(null, updateWrapper);
    
            //将所有文件重新命名
            for(FileInfo item : fileInfoList) {
                //文件名存在，重命名被还原文件
                FileInfo rootFileInfo = rootMap.get(item.getFileName());
                if(rootFileInfo != null) {
                    String FileName=StringTools.rename(item.getFileName());
                    FileInfo reNameFileInfo = new FileInfo();
                    reNameFileInfo.setFileName(FileName);
                    UpdateWrapper<FileInfo> reNameWrapper = new UpdateWrapper<>();
                    reNameWrapper.eq("user_id", userId)
                                    .eq("file_id",item.getFileId());
                    fileMapper.update(reNameFileInfo,reNameWrapper);
                }
            }
            
            return RestBean.success();
        } catch (Exception e) {
            log.error("恢复文件失败: {}", e.getMessage(), e);
            return RestBean.failure(500, "恢复文件失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RestBean delFileBatch(Integer userId, String fileIds, boolean adminOp) {
        // 检查文件ID是否为空
        if (fileIds == null || fileIds.trim().isEmpty()) {
            return RestBean.failure(600, "参数错误");
        }
        
        try {
            String[] fileIdArray = fileIds.split(",");
    
            FileInfoQuery query = new FileInfoQuery();
            QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId)
                    .in("file_id", fileIdArray);
    
            if (!adminOp) {
                queryWrapper.eq("del_flag", FileDelFlagEnums.RECYCLE.getFlag());
            }
    
            List<FileInfo>fileInfoList = fileMapper.selectList(queryWrapper);
            if (fileInfoList.isEmpty()) {
                return RestBean.failure(600, "文件不存在");
            }
    
            List<String> delFileSubFolderFileIdList = new ArrayList<>();
    
            //找到所选文件子目录文件ID
            for (FileInfo fileInfo : fileInfoList) {
                if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
                    findAllSubFolderFileIdList(delFileSubFolderFileIdList, userId, fileInfo.getFileId(), FileDelFlagEnums.DEL.getFlag());
                }
            }
            
            //删除所选文件，子目录中的文件
            if (!delFileSubFolderFileIdList.isEmpty()) {
                UpdateWrapper<FileInfo> updateWrapper = new UpdateWrapper<>();
                updateWrapper.in("file_id", delFileSubFolderFileIdList)
                        .eq("user_id", userId);
                if (!adminOp) {
                    updateWrapper.eq("del_flag", FileDelFlagEnums.DEL.getFlag());
                }
                fileMapper.delete(updateWrapper);
            }
            
            //删除所选文件
            UpdateWrapper<FileInfo> updateWrapper = new UpdateWrapper<>();
            updateWrapper.in("file_id", fileIdArray)
                    .eq("user_id", userId);
            if (!adminOp) {
                updateWrapper.eq("del_flag", FileDelFlagEnums.RECYCLE.getFlag());
            }
            fileMapper.delete(updateWrapper);
            
            return RestBean.success();
        } catch (Exception e) {
            log.error("彻底删除文件失败: {}", e.getMessage(), e);
            return RestBean.failure(500, "彻底删除文件失败: " + e.getMessage());
        }
    }

    @Override
    public RestBean getFolderInfo(String path, Integer userId) {
        String[] pathArray = path.split("/");
        FileInfoQuery infoQuery = new FileInfoQuery();
        infoQuery.setUserId(userId);
        infoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        infoQuery.setFileIdArray(pathArray);
        String orderBy = "field(file_id,\"" + org.apache.commons.lang3.StringUtils.join(pathArray, "\",\"") + "\")";
        infoQuery.setOrderBy(orderBy);
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(infoQuery);
        return RestBean.success(beanCopyUtils.copyBeanList(fileInfoList, FolderVO.class));
    }

    @Override
    public FileInfo getFileInfoByFileIdAndUserId(String fileId, Integer userId) {
           QueryWrapper<FileInfo> wrapper=new QueryWrapper<>();
           wrapper.eq("user_id", userId)
                   .eq("file_id", fileId);
           return fileMapper.selectOne(wrapper);

    }


    @Override
    public void checkRootFilePid(String rootFilePid, Integer userId, String fileId) {
        if (StringTools.isEmpty(fileId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (rootFilePid.equals(fileId)) {
            return;
        }
        checkFilePid(rootFilePid, fileId, userId);
    }



    @Override
    @Transactional
    public void saveShare(String shareRootFilePid, String shareFileIds, String myFolderId, Integer shareUserId, Integer cureentUserId) {
        String[] shareFileIdArray = shareFileIds.split(",");
        //目标目录文件列表

        QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", cureentUserId)
                .eq("file_pid",myFolderId);
        List<FileInfo> currentFileList = this.fileMapper.selectList(queryWrapper);
        Map<String, FileInfo> currentFileMap = currentFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));
        //选择的文件

        QueryWrapper<FileInfo>wrapper=new QueryWrapper<>();
        wrapper.eq("user_id", shareUserId)
                .in("file_id", shareFileIdArray);
        List<FileInfo> shareFileList = this.fileMapper.selectList(wrapper);
        //重命名选择的文件
        List<FileInfo> copyFileList = new ArrayList<>();
        Date curDate = new Date();
        for (FileInfo item : shareFileList) {
            FileInfo haveFile = currentFileMap.get(item.getFileName());
            if (haveFile != null) {
                item.setFileName(StringTools.rename(item.getFileName()));
            }
            findAllSubFile(copyFileList, item, shareUserId, cureentUserId, curDate, myFolderId);
        }

        // 批量插入
        fileInfoService.saveBatch(copyFileList);

        //更新空间
        Long useSpace = this.fileMapper.selectUserSpace(cureentUserId);
        Account dbUserInfo = this.userMapper.selectById(cureentUserId);
        if (useSpace > dbUserInfo.getTotalSpace()) {
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
       Account userInfo=new Account();

        userInfo.setUseSpace(useSpace);

        UpdateWrapper<Account>updateWrapper=new UpdateWrapper<>();
        updateWrapper.eq("user_id", cureentUserId)
                .set("userspace", useSpace);
        //设置缓存
        UserSpaceDTO userSpaceDto = redisComponent.getUserSpaceUse(cureentUserId);
        userSpaceDto.setUseSpace(useSpace);
        redisComponent.saveUserSpaceUse(cureentUserId, userSpaceDto);
    }
    private void findAllSubFile(List<FileInfo> copyFileList, FileInfo parentFileInfo, Integer sourceUserId, Integer targetUserId, Date curDate, String newParentId) {

        QueryWrapper<FileInfo>wrapper=new QueryWrapper<>();
        wrapper.eq("user_id", sourceUserId)
                .eq("file_pid", parentFileInfo.getFileId())
                .eq("del_flag", FileDelFlagEnums.USING.getFlag());
        List<FileInfo> subFileList = this.fileMapper.selectList(wrapper);

        for (FileInfo subFile : subFileList) {
            FileInfo newSubFile = new FileInfo();
            beanCopyUtils.copyBean(subFile, newSubFile.getClass());
            newSubFile.setFileId(StringTools.getRandomString(Constants.LENGTH_10));
            newSubFile.setUserId(targetUserId);
            newSubFile.setFilePid(newParentId);
            newSubFile.setCreateTime(curDate);
            newSubFile.setUpdateTime(curDate);

            copyFileList.add(newSubFile);

            // 如果是文件夹，继续递归
            if (FileFolderTypeEnums.FOLDER.getType().equals(subFile.getFolderType())) {
                findAllSubFile(copyFileList, subFile, sourceUserId, targetUserId, curDate, newSubFile.getFileId());
            }
        }
    }
    private void checkFilePid(String rootFilePid, String fileId, Integer userId) {
        FileInfo fileInfo = this.fileMapper.selectByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (Constants.ZERO_STR.equals(fileInfo.getFilePid())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (fileInfo.getFilePid().equals(rootFilePid)) {
            return;
        }
        checkFilePid(rootFilePid, fileInfo.getFilePid(), userId);
    }

    /**
     * 更新用户已使用空间
     * @param userId 用户ID
     * @param useSpace 已使用空间大小
     */
    private void updateUserUsedSpace(Integer userId, Long useSpace) {
        // 方式1：使用UpdateWrapper
        UpdateWrapper<Account> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userId)
                .set("user_space", useSpace);
        userMapper.update(null, updateWrapper);


    }
    /**
     * 查询用户已使用空间
     * @param userId 用户ID
     * @return 已使用空间大小（字节）
     */
    private Long queryUserUsedSpace(Integer userId) {
        // 使用MyBatis-Plus的聚合函数查询
        QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("IFNULL(SUM(file_size), 0) as total_size")
                .eq("user_id", userId)
                .eq("del_flag", FileDelFlagEnums.USING.getFlag());

        // 执行查询并获取结果
        Map<String, Object> resultMap = fileMapper.selectMaps(queryWrapper).get(0);
        return ((Number) resultMap.get("total_size")).longValue();


    }


    private void findAllSubFolderFileIdList(List<String> delFilePidList, Integer userId, String fileId, Integer flag) {
         delFilePidList.add(fileId);
         QueryWrapper<FileInfo> queryWrapper = new QueryWrapper<>();
         queryWrapper.eq("user_id", userId)
                 .eq("file_pid", fileId)
                 .eq("del_flag", flag)
                 .eq("file_type",FileFolderTypeEnums.FOLDER.getType());
          List<FileInfo> fileInfoList = fileMapper.selectList(queryWrapper);
          for (FileInfo fileInfo : fileInfoList) {
              findAllSubFolderFileIdList(delFilePidList, userId, fileInfo.getFileId(),flag);
          }

    }


    private Boolean checkFileName(String filePid, Integer userId, String fileName, Integer folderType) {
        if (StringUtils.isEmpty(fileName)) {
            return false;
        }
        // 创建查询条件
        QueryWrapper<FileInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("file_pid", filePid)
                .eq("user_id", userId)
                .eq("file_name", fileName)
                .eq("folder_type", folderType)
                .eq("del_flag", FileDelFlagEnums.USING.getFlag());

        // 查询数据库中是否存在符合条件的记录
        Integer count = Math.toIntExact(fileMapper.selectCount(wrapper));
        return count == 0;
    }

    private String autoRename(String filePid, Integer userId, String fileName) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfoQuery.setFileName(fileName);
        
        // 调用selectCount方法
        Integer count = this.selectCount(fileInfoQuery);
        
        if (count > 0) {
            fileName=StringTools.rename(fileName);
        }

        return fileName;
    }
    private void updateUserSpace(Integer userId, Long totalSize) {
        Integer count = userMapper.updateUserSpace(userId, totalSize, null);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
        UserSpaceDTO spaceDto = redisComponent.getUserSpaceUse(userId);
        spaceDto.setUseSpace(spaceDto.getUseSpace() + totalSize);
        redisComponent.saveUserSpaceUse(userId, spaceDto);
    }
    /**
     * 查询满足条件的记录数量
     * @param query 查询条件
     * @return 记录数量
     */
    public Integer selectCount(FileInfoQuery query) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        
        // 使用Optional和方法引用简化条件构建
        Optional.ofNullable(query.getUserId())
                .ifPresent(userId -> wrapper.eq(FileInfo::getUserId, userId));
        
        Optional.ofNullable(query.getFileId())
                .filter(id -> !id.isEmpty())
                .ifPresent(fileId -> wrapper.eq(FileInfo::getFileId, fileId));
        
        Optional.ofNullable(query.getFileName())
                .filter(name -> !name.isEmpty())
                .ifPresent(name -> wrapper.eq(FileInfo::getFileName, name));
        
        Optional.ofNullable(query.getFilePid())
                .filter(pid -> !pid.isEmpty())
                .ifPresent(pid -> wrapper.eq(FileInfo::getFilePid, pid));
        
        Optional.ofNullable(query.getDelFlag())
                .ifPresent(flag -> wrapper.eq(FileInfo::getDelFlag, flag));
        
        Optional.ofNullable(query.getFileCategory())
                .ifPresent(category -> wrapper.eq(FileInfo::getFileCategory, category));
        
        Optional.ofNullable(query.getFolderType())
                .ifPresent(type -> wrapper.eq(FileInfo::getFolderType, type));
        
        // 返回查询结果计数
        return Math.toIntExact(fileMapper.selectCount(wrapper));
    }

    //转码操作
    @Async
    public void transferFile(String fileId,Integer userid) {
        Boolean transferSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnums fileTypeEnum = null;
        FileInfo fileInfo = fileMapper.selectByFileIdAndUserId(fileId, userid);
        try {//状态必须是转码中
            if (fileInfo == null || !FileStatusEnums.TRANSFER.getStatus().equals(fileInfo.getStatus())) {
                return;
            }
            //临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName =userid.toString() + fileId;
            File fileFolder = new File(tempFolderName + currentUserFolderName);
            if (!fileFolder.exists()) {
                fileFolder.mkdirs();
            }
            //文件后缀
            String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
            String month =DateUtils.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());
            //目标目录
            String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFolder = new File(targetFolderName + "/" + month);
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            //真实文件名
            String realFileName = currentUserFolderName + fileSuffix;
            //真实文件路径
            targetFilePath = targetFolder.getPath() + "/" + realFileName;
            //合并文件
            union(fileFolder.getPath(), targetFilePath, fileInfo.getFileName(), true);
            //视频文件切割
            fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            if (FileTypeEnums.VIDEO == fileTypeEnum) {
                cutFile4Video(fileId, targetFilePath);
                //视频生成缩略图
                cover = month + "/" + currentUserFolderName + Constants.IMAGE_PNG_SUFFIX;
                String coverPath = targetFolderName + "/" + cover;
                ScaleFilter.createCover4Video(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath));
            } else if (FileTypeEnums.IMAGE == fileTypeEnum) {
                //生成缩略图
                cover = month + "/" + realFileName.replace(".", "_.");
                String coverPath = targetFolderName + "/" + cover;
                Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath), false);
                if (!created) {
                    FileUtils.copyFile(new File(targetFilePath), new File(coverPath));
                }
            }
        } catch (Exception e) {
            logger.error("文件转码失败，文件Id:{},userId:{}", fileId, userid, e);
            transferSuccess = false;
        } finally {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setFileSize(new File(targetFilePath).length());
            updateInfo.setFileCover(cover);
            updateInfo.setStatus(transferSuccess ? FileStatusEnums.USING.getStatus() : FileStatusEnums.TRANSFER_FAIL.getStatus());
            fileMapper.updateFileStatusWithOldStatus(fileId, userid, updateInfo, FileStatusEnums.TRANSFER.getStatus());
        }
    }

    public static void union(String dirPath, String toFilePath, String fileName, boolean delSource) throws BusinessException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        //将所有需要合并的文件拿到
        File fileList[] = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;
        InternalLogger logger = null;
        try {
            writeFile = new RandomAccessFile(targetFile, "rw");
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                //创建读块文件的对象
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    logger.error("合并分片失败", e);
                    throw new BusinessException("合并文件失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            logger.error("合并文件:{}失败", fileName, e);
            throw new BusinessException("合并文件" + fileName + "出错了");
        } finally {
            try {
                if (null != writeFile) {
                    writeFile.close();
                }
            } catch (IOException e) {
                logger.error("关闭流失败", e);
            }
            if (delSource) {
                if (dir.exists()) {
                    try {
                        FileUtils.deleteDirectory(dir);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    private void cutFile4Video(String fileId, String videoFilePath) {
        //创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }

        final String CMD_GET_CODE = "ffprobe -v error -select_streams v:0 -show_entries stream=codec_name %s";
        String cmd = String.format(CMD_GET_CODE, videoFilePath);
        String result = ProcessUtils.executeCommand(cmd, false);
        result = result.replace("\n", "");
        result = result.substring(result.indexOf("=") + 1);
        String codec = result.substring(0, result.indexOf("["));

        //转码
        if ("hevc".equals(codec)) {
            String newFileName = videoFilePath.substring(0, videoFilePath.lastIndexOf(".")) + "_" + videoFilePath.substring(videoFilePath.lastIndexOf("."));
            new File(videoFilePath).renameTo(new File(newFileName));
            String CMD_HEVC_264 = "ffmpeg -i %s -c:v libx264 -crf 20 %s";
            cmd = String.format(CMD_HEVC_264, newFileName, videoFilePath);
            ProcessUtils.executeCommand(cmd, false);
            new File(newFileName).delete();
        }

        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy -bsf:v h264_mp4toannexb %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";

        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        //生成.ts
        cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, false);
        //生成索引文件.m3u8 和切片.ts
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        ProcessUtils.executeCommand(cmd, false);
        //删除index.ts
        new File(tsPath).delete();
    }

    protected void readFile(HttpServletResponse response, String filePath) {
        if (!StringTools.pathIsOk(filePath)) {
            return;
        }
        OutputStream out = null;
        FileInputStream in = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
            in = new FileInputStream(file);
            byte[] byteData = new byte[1024];
            out = response.getOutputStream();
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            logger.error("读取文件异常", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.error("IO异常", e);
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("IO异常", e);
                }
            }
        }
    }
    protected void readFile(HttpServletResponse response, HttpServletRequest request, String filePath) {
        if (!StringTools.pathIsOk(filePath)) {
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }

        // 获取文件类型（扩展名）
        String fileName = file.getName().toLowerCase();

        // 设置跨域响应头，解决跨域问题
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Range, If-Modified-Since, If-None-Match");
        response.setHeader("Access-Control-Expose-Headers", "Content-Range, Accept-Ranges, Content-Length, Content-Type");
        response.setHeader("Access-Control-Max-Age", "3600");

        // 设置支持断点续传的响应头
        response.setHeader("Accept-Ranges", "bytes");

        // 根据文件类型设置正确的Content-Type
        boolean isVideo = fileName.endsWith(".mp4") || fileName.endsWith(".ts");
        
        if (fileName.endsWith(".m3u8")) {
            response.setContentType("application/vnd.apple.mpegurl");
            // m3u8文件不缓存
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        } else if (fileName.endsWith(".ts")) {
            response.setContentType("video/mp2t");
            response.setHeader("Cache-Control", "public, max-age=31536000");
        } else if (fileName.endsWith(".mp4")) {
            response.setContentType("video/mp4");
            response.setHeader("Cache-Control", "public, max-age=31536000");
        } else if (fileName.endsWith(".pdf")) {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=" + fileName);
        } else if (fileName.endsWith(".pptx")) {
            response.setContentType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
            response.setHeader("Content-Disposition", "inline; filename=" + fileName);
        } else {
            response.setContentType("application/octet-stream");
        }
        
        if (isVideo) {
            // 视频文件的额外响应头
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Connection", "keep-alive");
        }

        // 获取文件长度
        long fileLength = file.length();
        String rangeHeader = request != null ? request.getHeader("Range") : null;
        long startByte = 0;
        long endByte = fileLength - 1;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            try {
                String[] ranges = rangeHeader.substring("bytes=".length()).split("-");
                
                // 处理起始字节
                if (!ranges[0].isEmpty()) {
                    try {
                        startByte = Long.parseLong(ranges[0]);
                    } catch (NumberFormatException e) {
                        startByte = 0;
                    }
                }
                
                // 处理结束字节
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    try {
                        endByte = Long.parseLong(ranges[1]);
                    } catch (NumberFormatException e) {
                        endByte = fileLength - 1;
                    }
                } else if (isVideo) {
                    // 对于视频文件，如果没有指定结束字节，限制每次返回的大小
                    endByte = Math.min(startByte + (2 * 1024 * 1024), fileLength - 1); // 每次返回2MB
                }

                // 处理负数范围（例如：bytes=-500）
                if (ranges[0].isEmpty() && ranges.length > 1 && !ranges[1].isEmpty()) {
                    try {
                        long length = Long.parseLong(ranges[1]);
                        startByte = Math.max(0, fileLength - length);
                        endByte = fileLength - 1;
                    } catch (NumberFormatException e) {
                        startByte = 0;
                        endByte = fileLength - 1;
                    }
                }

                // 验证和调整范围的有效性
                if (startByte < 0) startByte = 0;
                if (endByte >= fileLength) endByte = fileLength - 1;
                
                // 如果起始位置超过文件长度，返回 416
                if (startByte >= fileLength) {
                    response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    response.setHeader("Content-Range", "bytes */" + fileLength);
                    return;
                }

                // 如果结束位置小于起始位置，返回 416
                if (endByte < startByte) {
                    response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    response.setHeader("Content-Range", "bytes */" + fileLength);
                    return;
                }

                // 限制单次传输的最大大小
                if (isVideo && (endByte - startByte) > (10 * 1024 * 1024)) { // 最大10MB
                    endByte = startByte + (10 * 1024 * 1024) - 1;
                }

                // 设置206状态码（部分内容）
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + startByte + "-" + endByte + "/" + fileLength);
                response.setHeader("Content-Length", String.valueOf(endByte - startByte + 1));
            } catch (Exception e) {
                logger.error("处理Range请求异常", e);
                // 如果解析Range失败，返回整个文件
                startByte = 0;
                endByte = fileLength - 1;
                response.setHeader("Content-Length", String.valueOf(fileLength));
            }
        } else {
            // 不是Range请求，返回整个文件
            response.setHeader("Content-Length", String.valueOf(fileLength));
        }

                    // 使用缓冲流提高性能
            try (BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream(), 32768); // 32KB 输出缓冲
                 RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
                
                randomAccessFile.seek(startByte);
                byte[] buffer = new byte[32768]; // 32KB 读取缓冲
                long bytesToRead = endByte - startByte + 1;
                int bytesRead;

                while (bytesToRead > 0 && (bytesRead = randomAccessFile.read(buffer, 0, (int) Math.min(buffer.length, bytesToRead))) != -1) {
                    out.write(buffer, 0, bytesRead);
                    bytesToRead -= bytesRead;
                    
                    // 对于视频文件，定期刷新缓冲区以确保流畅播放
                    if (isVideo && bytesToRead % (128 * 1024) == 0) { // 每128KB刷新一次
                        out.flush();
                    }
                }
                out.flush();
            } catch (Exception e) {
                logger.error("读取文件异常: {}", e.getMessage(), e);
                if (!response.isCommitted()) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public UploadResultDto uploadFile(Integer userId, FileUploadVO uploadVO) throws IOException {
        return uploadFile(userId, 
                          uploadVO.getFileId(), 
                          uploadVO.getFile(), 
                          uploadVO.getFileName(), 
                          uploadVO.getFilePid(), 
                          uploadVO.getFileMd5(), 
                          uploadVO.getChunkIndex(), 
                          uploadVO.getChunks());
    }

}
