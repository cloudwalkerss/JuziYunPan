package com.example.controller;

import com.example.constants.Const;
import com.example.constants.Constants;
import com.example.entity.Dto.Account;
import com.example.entity.Dto.FileInfo;
import com.example.entity.Dto.FileShare;
import com.example.entity.Enum.FileDelFlagEnums;
import com.example.entity.Enum.ResponseCodeEnum;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.FileInfoQuery;
import com.example.entity.Vo.response.FileInfoVO;
import com.example.entity.Vo.response.PaginationResultVO;
import com.example.entity.Vo.response.ShareInfoVO;
import com.example.exception.BusinessException;
import com.example.mapper.UserMapper;
import com.example.service.FileService;
import com.example.service.FileShareService;
import com.example.service.userService;
import com.example.utils.BeanCopyUtils;
import com.example.utils.JwtUtils;
import com.example.utils.StringTools;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;


import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/showShare")
public class WebShareController {
      @Autowired
      FileShareService fileShareService;

      @Autowired
    FileService fileService;

      @Autowired
    userService  userService;

      @Autowired
    BeanCopyUtils beanCopyUtils;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    RedisTemplate redisTemplate;
    /**
     * 获取分享登录信息
     *
     * @param userId 当前登录用户ID（可能为null）
     * @param shareId 分享ID
     * @return 分享信息
     */
    @RequestMapping("/getShareLoginInfo")
    public RestBean getShareLoginInfo(@RequestAttribute(value = Const.ATTR_USER_ID, required = false) Integer userId,
                                      String shareId) {
        // 验证分享是否有效
        try {
            // 获取分享详情
            ShareInfoVO shareInfoVO = getShareInfoCommon(shareId);

            // 判断当前访问者是否是分享的创建者
            if (userId != null && userId.equals(shareInfoVO.getUserId())) {
                shareInfoVO.setCurrentUser(true);
            } else {
                shareInfoVO.setCurrentUser(false);
            }

            return RestBean.success(shareInfoVO);
        } catch (BusinessException e) {
            // 分享无效或已过期
            return RestBean.failure(800,e.getMessage());
        } catch (Exception e) {
            return RestBean.failure(802,e.getMessage());
        }
    }
    /**
     * 获取分享信息
     *
     * @param shareId
     * @return
     */
    @RequestMapping("/getShareInfo")

    public RestBean getShareInfo( String shareId) {
        return RestBean.success(getShareInfoCommon(shareId));
    }

    private ShareInfoVO getShareInfoCommon(String shareId) {
        FileShare share = fileShareService.getFileShareByShareId(shareId);

        if (null == share || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }

        ShareInfoVO shareInfoVO = beanCopyUtils.copyBean(share, ShareInfoVO.class);
        //查到分享文件
        FileInfo fileInfo = fileService.getFileInfoByFileIdAndUserId(share.getFileId(), share.getUserId());

        if (fileInfo == null || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }

        shareInfoVO.setFileName(fileInfo.getFileName());
        //分享人的用户信息

        Account userInfo = userMapper.findById(share.getUserId());


        shareInfoVO.setNickName(userInfo.getNickname());
        shareInfoVO.setAvatar(userInfo.getAvatar());
        shareInfoVO.setUserId(userInfo.getId());
        return shareInfoVO;
    }
    /**
     * 验证分享密码
     *
     * @param shareId 分享ID
     * @param code 分享密码
     * @return 验证结果
     */
    @RequestMapping("/checkShareCode")
    public RestBean checkShareCode(String shareId, String code) {
        FileShare share = fileShareService.getFileShareByShareId(shareId);

        if (null == share || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))) {
             throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }

        // 验证密码
        if (share.getCode() != null && !share.getCode().equals(code)) {
           return  RestBean.failure(803,"分享验证码错误");
        }
         fileShareService.updateShareCount(shareId);
        // 生成临时访问令牌
        String token = jwtUtils.createShareAccessToken(shareId, share.getUserId());


        return RestBean.success(token);
    }


    /**
     * 获取文件列表
     *
     * @param shareId 分享ID
     * @param filePid 父文件ID
     * @return 文件列表
     */
    @RequestMapping("/loadFileList")
    public RestBean loadFileList(@RequestAttribute("shareId") String shareId,
                                 @RequestAttribute("shareUserId") Integer shareUserId,
                                 String filePid) {
        // 增加Redis中的访问验证
        String validKey = Const.SHARE_ACCESS_TOKEN_VERSION + shareId;
        String validToken = (String) redisTemplate.opsForValue().get(validKey);
        if (validToken == null) {
            return RestBean.failure(804, "分享已失效，请重新获取");
        }
        try {
            FileInfoQuery query = new FileInfoQuery();

            // 获取分享的文件信息
            FileShare share = fileShareService.getFileShareByShareId(shareId);
            if (share == null) {
                throw  new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
            }

            // 判断是查询根目录还是子目录
            if (!StringTools.isEmpty(filePid) && !Constants.ZERO_STR.equals(filePid)) {
                // 检查请求的目录是否属于分享范围内
                fileService.checkRootFilePid(share.getFileId(), shareUserId, filePid);
                query.setFilePid(filePid);
            } else {
                // 如果是查询根目录，直接使用分享的文件ID
                query.setFileId(share.getFileId());
            }

            // 设置查询条件
            query.setUserId(shareUserId);
            query.setOrderBy("update_time desc");
            query.setDelFlag(FileDelFlagEnums.USING.getFlag());

            // 查询文件列表
            PaginationResultVO resultVO = fileService.findListByPage(query);

            // 转换结果并返回
            List fileInfoVOList = beanCopyUtils.copyBeanList(resultVO.getList(), FileInfoVO.class);

            // 构建分页结果
            PaginationResultVO<FileInfoVO> result = new PaginationResultVO<>();
            result.setList(fileInfoVOList);
            result.setPageNo(resultVO.getPageNo());
            result.setPageSize(resultVO.getPageSize());
            result.setPageTotal(resultVO.getPageTotal());
            result.setTotalCount(resultVO.getTotalCount());

            return RestBean.success(result);
        } catch (BusinessException e) {
            return RestBean.failure(804,e.getMessage());
        } catch (Exception e) {
            return RestBean.failure(805,e.getMessage());
        }
    }

    /**
     * 获取目录信息
     *
     * @param shareId 分享ID
     * @param path 路径参数
     * @return 目录信息
     */
    @RequestMapping("/getFolderInfo")
    public RestBean getFolderInfo(@RequestAttribute("shareId") String shareId,
                                  @RequestAttribute("shareUserId") Integer shareUserId,
                                  String path) {
        try {
            // 增加Redis中的访问验证
            String validKey = Const.SHARE_ACCESS_TOKEN_VERSION + shareId;
            String validToken = (String) redisTemplate.opsForValue().get(validKey);
            if (validToken == null) {
                return RestBean.failure(804, "分享已失效，请重新获取");
            }

            // 获取分享信息
            FileShare share = fileShareService.getFileShareByShareId(shareId);
            if (share == null) {
                return RestBean.failure(804, "分享不存在");
            }

            // 调用fileService中的getFolderInfo方法获取文件夹信息
            return fileService.getFolderInfo(path, shareUserId);
        } catch (BusinessException e) {
            return RestBean.failure(804, e.getMessage());
        } catch (Exception e) {
            return RestBean.failure(805, e.getMessage());
        }
    }

    /**
     * 获取文件
     */
    @RequestMapping("/getFile/{shareId}/{fileId}")
    public void getFile(HttpServletRequest request, HttpServletResponse response,
                        @PathVariable("shareId")  String shareId,
                        @PathVariable("fileId") String fileId) {
        try {
            // 验证分享有效性
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // 解析JWT令牌
            Map<String, Object> tokenInfo = jwtUtils.resolveShareToken(token.substring(7));
            if (tokenInfo == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String tokenShareId = (String) tokenInfo.get("shareId");
            Integer shareUserId = (Integer) tokenInfo.get("shareUserId");

            // 验证shareId是否匹配
            if (!shareId.equals(tokenShareId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // 增加Redis中的访问验证
            String validKey = Const.SHARE_ACCESS_TOKEN_VERSION + shareId;
            String validToken = (String) redisTemplate.opsForValue().get(validKey);
            if (validToken == null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // 调用文件服务获取文件
            fileService.getFIle(request,response, fileId, shareUserId);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取视频信息
     */
    @RequestMapping("/ts/getVideoInfo/{shareId}/{fileId}")
    public void getVideoInfo(HttpServletRequest request, HttpServletResponse response,
                             @PathVariable("shareId")  String shareId,
                             @PathVariable("fileId")  String fileId) {
        try {
            // 验证分享有效性
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // 解析JWT令牌
            Map<String, Object> tokenInfo = jwtUtils.resolveShareToken(token.substring(7));
            if (tokenInfo == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String tokenShareId = (String) tokenInfo.get("shareId");
            Integer shareUserId = (Integer) tokenInfo.get("shareUserId");

            // 验证shareId是否匹配
            if (!shareId.equals(tokenShareId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // 增加Redis中的访问验证
            String validKey = Const.SHARE_ACCESS_TOKEN_VERSION + shareId;
            String validToken = (String) redisTemplate.opsForValue().get(validKey);
            if (validToken == null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // 调用文件服务获取视频信息
            fileService.getFIle(request,response, fileId, shareUserId);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 创建下载链接
     */
    @RequestMapping("/createDownloadUrl/{shareId}/{fileId}")
    public RestBean createDownloadUrl(HttpServletRequest request,
                                      @PathVariable("shareId")  String shareId,
                                      @PathVariable("fileId")  String fileId) {
        try {
            // 验证分享有效性
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                return RestBean.failure(401, "未提供有效的访问令牌");
            }

            // 解析JWT令牌
            Map<String, Object> tokenInfo = jwtUtils.resolveShareToken(token.substring(7));
            if (tokenInfo == null) {
                return RestBean.failure(401, "访问令牌无效");
            }

            String tokenShareId = (String) tokenInfo.get("shareId");
            Integer shareUserId = (Integer) tokenInfo.get("shareUserId");

            // 验证shareId是否匹配
            if (!shareId.equals(tokenShareId)) {
                return RestBean.failure(403, "分享ID不匹配");
            }

            // 增加Redis中的访问验证
            String validKey = Const.SHARE_ACCESS_TOKEN_VERSION + shareId;
            String validToken = (String) redisTemplate.opsForValue().get(validKey);
            if (validToken == null) {
                return RestBean.failure(403, "分享已失效，请重新获取");
            }

            // 创建下载链接
            return fileService.createDownloadUrl(shareUserId, fileId);
        } catch (BusinessException e) {
            return RestBean.failure(804, e.getMessage());
        } catch (Exception e) {
            return RestBean.failure(805, e.getMessage());
        }
    }

    /**
     * 下载文件
     */
    @RequestMapping("/download/{code}")
    public void download(HttpServletRequest request, HttpServletResponse response,
                         @PathVariable("code")  String code) throws Exception {
        // 下载功能通常使用一次性的下载码，不需要JWT验证
        // 直接调用下载服务
        fileService.download(request, response, code);
    }

    /**
     * 保存分享到自己的网盘
     */
    @RequestMapping("/saveShare")
    public RestBean saveShare(HttpServletRequest request,
                              String shareId,
                              String shareFileIds,
                              String myFolderId) {
        try {
            // 1. 验证分享访问令牌
            String shareToken = request.getHeader("Authorization");
            if (shareToken == null || !shareToken.startsWith("Bearer ")) {
                return RestBean.failure(401, "未提供有效的分享访问令牌");
            }

            // 解析分享JWT令牌
            Map<String, Object> shareTokenInfo = jwtUtils.resolveShareToken(shareToken.substring(7));
            if (shareTokenInfo == null) {
                return RestBean.failure(401, "分享访问令牌无效");
            }

            String tokenShareId = (String) shareTokenInfo.get("shareId");
            Integer shareUserId = (Integer) shareTokenInfo.get("shareUserId");

            // 验证shareId是否匹配
            if (!shareId.equals(tokenShareId)) {
                return RestBean.failure(403, "分享ID不匹配");
            }

            // 增加Redis中的访问验证
            String validKey = Const.SHARE_ACCESS_TOKEN_VERSION + shareId;
            String validToken = (String) redisTemplate.opsForValue().get(validKey);
            if (validToken == null) {
                return RestBean.failure(403, "分享已失效，请重新获取");
            }

            // 2. 验证当前用户登录令牌
            String userToken = request.getHeader("User-Authorization");
            if (userToken == null || !userToken.startsWith("Bearer ")) {
                return RestBean.failure(401, "请先登录");
            }

            // 解析用户JWT令牌
            Integer currentUserId = jwtUtils.toId(jwtUtils.resolveJwt(userToken));
            if (currentUserId == null) {
                return RestBean.failure(401, "登录已过期，请重新登录");
            }

            // 3. 验证不能保存自己分享的文件
            if (shareUserId.equals(currentUserId)) {
                return RestBean.failure(500, "自己分享的文件无法保存到自己的网盘");
            }

            // 4. 获取分享根文件ID
            FileShare share = fileShareService.getFileShareByShareId(shareId);
            if (share == null) {
                return RestBean.failure(404, "分享不存在");
            }

            // 5. 保存分享文件
            fileService.saveShare(share.getFileId(), shareFileIds, myFolderId, shareUserId, currentUserId);

            return RestBean.success();
        } catch (BusinessException e) {
            return RestBean.failure(804, e.getMessage());
        } catch (Exception e) {
            return RestBean.failure(805, e.getMessage());
        }
    }

}


