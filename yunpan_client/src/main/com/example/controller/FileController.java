package com.example.controller;

import com.example.constants.Const;
import com.example.constants.Constants;
import com.example.entity.Dto.FileInfo;
import com.example.entity.Dto.UploadResultDto;
import com.example.entity.Enum.FileCategoryEnums;
import com.example.entity.Enum.FileDelFlagEnums;
import com.example.entity.Enum.FileFolderTypeEnums;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.FileInfoQuery;
import com.example.entity.Vo.request.createFolderVo;
import com.example.entity.Vo.request.reNameVo;
import com.example.entity.Vo.request.FileUploadVO;
import com.example.entity.Vo.request.MoveFileVo;
import com.example.entity.Vo.request.DeleteFileVo;
import com.example.entity.Vo.request.LoadFolderVo;
import com.example.entity.Vo.response.FileInfoVO;
import com.example.entity.Vo.response.FolderVO;
import com.example.entity.Vo.response.PaginationResultVO;
import com.example.exception.BusinessException;
import com.example.service.FileService;
import com.example.utils.BeanCopyUtils;
import com.example.utils.StringTools;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 文件控制器
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    private final String attrUserId = Const.ATTR_USER_ID;
    @Autowired
    private FileService fileService;
    @Autowired
    BeanCopyUtils beanCopyUtils;
    /**
     * 项目文件存储根目录
     */
    @Value("${project.folder}")
    private String projectFolder;

    /**
     * 获取图片接口
     *
     * @param response    HTTP响应对象，用于返回图片数据
     * @param imageFolder 图片所在文件夹名称
     * @param imageName   图片文件名
     */
    @GetMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response,
                         @PathVariable("imageFolder") String imageFolder,
                         @PathVariable("imageName") String imageName) {
        // 参数校验
        if (imageFolder == null || imageFolder.isEmpty() || imageName == null || imageName.isEmpty()) {
            log.warn("请求图片参数无效：imageFolder={}, imageName={}", imageFolder, imageName);
            return;
        }

        // 获取图片后缀并设置响应类型
        String imageSuffix = getFileSuffix(imageName).replace(".", "");
        String contentType = "image/" + imageSuffix;
        response.setContentType(contentType);

        // 设置缓存时间为30天
        response.setHeader("Cache-Control", "max-age=2592000");

        // 构建文件完整路径
        String filePath = projectFolder + "/file/" + imageFolder + "/" + imageName;

        // 读取并输出文件
        readFileToResponse(response, filePath);
    }

    /**
     * 工具方法：获取文件后缀名
     *
     * @param fileName 文件名
     * @return 文件后缀（包含点号）
     */
    private String getFileSuffix(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return "";
        }

        return fileName.substring(index);
    }

    /**
     * 工具方法：将文件内容读取到HTTP响应中
     *
     * @param response HTTP响应对象
     * @param filePath 文件完整路径
     */
    private void readFileToResponse(HttpServletResponse response, String filePath) {
        // 安全检查，防止路径遍历攻击
        if (!isPathSafe(filePath)) {
            log.error("检测到不安全的文件路径请求: {}", filePath);
            return;
        }

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            log.warn("请求的文件不存在: {}", filePath);
            return;
        }

        // 使用try-with-resources自动关闭流
        try (FileInputStream in = new FileInputStream(file);
             OutputStream out = response.getOutputStream()) {

            // 使用缓冲区读取文件并写入响应
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            out.flush();
        } catch (IOException e) {
            log.error("读取文件时发生IO异常: {}", filePath, e);
        }
    }

    /**
     * 工具方法：检查文件路径是否安全（防止目录遍历攻击）
     *
     * @param path 要检查的文件路径
     * @return 如果路径安全返回true，否则返回false
     */
    private boolean isPathSafe(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        // 防止目录遍历攻击
        if (path.contains("..") || path.contains("./") || path.contains("/.")) {
            return false;
        }

        // 检查是否是合法的文件路径
        File file = new File(path);
        try {
            // 获取规范路径并检查是否在允许的目录下
            String canonicalPath = file.getCanonicalPath();
            return canonicalPath.startsWith(new File(projectFolder).getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }

    @RequestMapping("/uploadFile")
    public RestBean uploadFile(@RequestAttribute(attrUserId) Integer userid,
                               @ModelAttribute FileUploadVO uploadVO) throws IOException {
        try {
            // 验证上传文件是否为空
            if (uploadVO.getFile() == null || uploadVO.getFile().isEmpty()) {
                return RestBean.failure(400, "上传文件不能为空");
            }
            
            // 验证文件类型是否允许上传
            if (uploadVO.getChunkIndex() == 0 && !StringTools.isAllowFileType(uploadVO.getFileName())) {
                return RestBean.failure(400, "不支持的文件类型");
            }
            
            // 验证文件大小
            if (uploadVO.getChunkIndex() == 0 && uploadVO.getFile().getSize() > Constants.MAX_FILE_SIZE) {
                return RestBean.failure(400, "文件大小超过限制，最大允许" + StringTools.formatFileSize(Constants.MAX_FILE_SIZE));
            }
            
            // 记录文件上传开始
            log.info("用户{}开始上传文件：{}, 当前分片：{}/{}, 文件大小：{}bytes", 
                      userid, uploadVO.getFileName(), uploadVO.getChunkIndex(), uploadVO.getChunks(), 
                      uploadVO.getFile().getSize());
            
            // 处理文件上传
            UploadResultDto resultDto = fileService.uploadFile(userid, uploadVO);
            
            // 记录上传结果
            log.info("用户{}上传文件：{}, 当前分片：{}/{}, 状态：{}", 
                      userid, uploadVO.getFileName(), uploadVO.getChunkIndex(), uploadVO.getChunks(), 
                      resultDto.getStatus());
                      
            return RestBean.success(resultDto);
        } catch (BusinessException e) {
            log.error("文件上传业务异常: {}", e.getMessage());
            return RestBean.failure(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("文件上传发生异常:", e);
            return RestBean.failure(500, "文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 根据条件分页查询文件列表
     */
    @RequestMapping("/loadFileList")
    public RestBean loadFileList(@RequestAttribute(Const.ATTR_USER_ID) Integer userid, String category, FileInfoQuery query) {
        // 处理文件类别
        FileCategoryEnums categoryEnums = FileCategoryEnums.getByCode(category);
        if (categoryEnums != null) {
            query.setFileCategory(categoryEnums.getCategory());//可能是有限制分类的查询，有可能是查询全部
        }

        // 设置查询条件
        query.setUserId(userid);
        query.setOrderBy("update_time desc"); // 按最后更新时间降序排序
        query.setDelFlag(FileDelFlagEnums.USING.getFlag()); // 仅查询使用中的文件

        // 调用服务层方法进行分页查询
        PaginationResultVO<FileInfoVO> result = fileService.findListByPage(query);

        // 将实体对象转换成VO对象
        return RestBean.success(result);
    }

//    /**
//     * 将分页结果转换为VO对象
//     */
//    private PaginationResultVO<FileInfoVO> convert2PaginationVO(PaginationResultVO<FileInfo> result) {
//        List<FileInfoVO> voList = new ArrayList<>();
//        if (result.getList() != null && !result.getList().isEmpty()) {
//            for (FileInfo fileInfo : result.getList()) {
//                FileInfoVO vo = beanCopyUtils.copyBean(fileInfo, FileInfoVO.class);
//                voList.add(vo);
//            }
//        }
//
//        PaginationResultVO<FileInfoVO> resultVO = new PaginationResultVO<>();
//        resultVO.setList(voList);
//        resultVO.setPageNo(result.getPageNo());
//        resultVO.setPageSize(result.getPageSize());
//        resultVO.setPageTotal(result.getPageTotal());
//        resultVO.setTotalCount(result.getTotalCount());
//
//        return resultVO;
//    }

    @RequestMapping("/ts/getVideoInfo/{fileId}")
    public void getVideoInfo(HttpServletResponse response, @RequestAttribute(Const.ATTR_USER_ID) Integer userId, @PathVariable("fileId") String fileId) throws IOException {

        fileService.getFIle(response, fileId, userId);
    }

    @RequestMapping("/getFile/{fileId}")
    public void getFile(@RequestAttribute(Const.ATTR_USER_ID) Integer userId, HttpServletResponse response, @PathVariable("fileId") String fileId) throws IOException {
         fileService.getFIle(response,fileId, userId);



    }

    //新建文件夹
    @RequestMapping("/newFolder")
    public RestBean newFoloder(@RequestAttribute(Const.ATTR_USER_ID) Integer userId,@RequestBody createFolderVo vo) {
        return fileService.newFolder(userId,vo);
    }

    //获取当前文件夹下面的内容
    @RequestMapping("/getFolderInfo")
    public RestBean getFolderInfo(String path, Integer userId) {
        return fileService.getFolderInfo(path, userId);
    }
    
    //重命名
    @RequestMapping("/rename")
    public RestBean rename(@RequestAttribute(Const.ATTR_USER_ID) Integer userId,@RequestBody reNameVo vo) {
        return fileService.rename(userId,vo);
    }

    @RequestMapping("/loadAllFolder")
    public RestBean loadAllFolder(@RequestAttribute(Const.ATTR_USER_ID) Integer userId, @RequestBody LoadFolderVo vo) {
         FileInfoQuery infoQuery = new FileInfoQuery();
         infoQuery.setUserId(userId);
         infoQuery.setFilePid(vo.getFilePId());
         infoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
         if(!StringUtils.isEmpty(vo.getExcludeFileIds())) {
             infoQuery.setExcludeFileIdArray(vo.getExcludeFileIds().split(","));
         }
         infoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
         infoQuery.setOrderBy("create_time desc");
         List<FileInfo> fileInfoList = fileService.findListByParam(infoQuery);
         return RestBean.success(beanCopyUtils.copyBeanList(fileInfoList, FolderVO.class));
    }
    
    @RequestMapping("/changeFileFolder")
    public RestBean changeFileFolder(@RequestAttribute(Const.ATTR_USER_ID) Integer userId, @RequestBody MoveFileVo vo) {
        return fileService.changeFileFolder(vo.getFileIds(), vo.getFilePid(), userId);
    }

    @RequestMapping("/createDownloadUrl/{fileId}")

    public RestBean createDownloadUrl(@RequestAttribute(Const.ATTR_USER_ID)Integer userId, @PathVariable("fileId")  String fileId) {
       return fileService.createDownloadUrl(userId,fileId);
    }

    @RequestMapping("/download/{code}")

    public void download(HttpServletRequest request, HttpServletResponse response, @PathVariable("code")  String code) throws Exception {
            fileService.download(request,response,code);

    }
    @RequestMapping("/delFile")
    public RestBean delFile(@RequestAttribute(Const.ATTR_USER_ID)Integer userId, @RequestBody DeleteFileVo vo) {
        return fileService.removeFile2RecycleBatch(userId, vo.getFileIds());
    }
}

