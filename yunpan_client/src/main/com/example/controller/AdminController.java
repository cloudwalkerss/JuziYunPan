package com.example.controller;


import com.example.component.RedisComponent;
import com.example.entity.Dto.SysSettingsDto;
import com.example.entity.Enum.FileDelFlagEnums;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.*;

import com.example.entity.Vo.response.PaginationResultVO;
import com.example.service.FileService;
import com.example.service.UserAdminService;
import com.example.service.userService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController("adminController")
@RequestMapping("/admin")
public class AdminController  {

    @Autowired
    private RedisComponent redisComponent;

    @Resource
    private userService userInfoService;

    @Resource
    private FileService fileInfoService;

    @Autowired
    private UserAdminService userAdminService;


    @RequestMapping("/getSysSettings")
    public RestBean getSysSettings() {
        return RestBean.success(
                redisComponent.getSysSettingsDto());
    }

    @RequestMapping("/saveSysSettings")
    public RestBean saveSysSettings(
          @RequestBody SysSettingsDto vo) {
        SysSettingsDto sysSettingsDto = new SysSettingsDto();
        sysSettingsDto.setUserInitUseSpace(vo.getUserInitUseSpace());
        redisComponent.saveSysSettingsDto(sysSettingsDto);
        return RestBean.success();
    }




    @RequestMapping("/loadUserList")
  //管理用户，显示用户的信息
    public RestBean loadUser(@RequestBody UserAdminQuery userAdminQuery) {

         return userAdminService.loadUser(userAdminQuery);
    }


    @RequestMapping("/updateUserStatus")

    public RestBean updateUserStatus(@RequestBody updateUserStaus updateUserStaus) {
        userAdminService.updateUserStatus(updateUserStaus.getUserId(),updateUserStaus.getBanned());
        return RestBean.success();
    }

    @RequestMapping("/updateUserSpace")
    public RestBean  updateUserSpace(@RequestBody updateUserSpaceVo vo) {
        userAdminService.changeUserSpace(vo.getUserId(), vo.getTotalSpace());

        return RestBean.success();
    }

    /**
     * 查询所有文件
     *
     * @param query
     * @return
     */
    @RequestMapping("/loadFileList")
    public RestBean loadDataList(@RequestBody FileInfoQuery query) {
        query.setOrderBy("update_time desc");
        query.setQueryNickName(true);
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        query.setFolderType(0);
        PaginationResultVO resultVO = fileInfoService.findListByPage(query);
        return RestBean.success(resultVO);
    }

    @RequestMapping("/getFolderInfo")
    public RestBean getFolderInfo( String path) {
        return fileInfoService.getFolderInfo(path,null);
    }


    @RequestMapping("/getFile/{userId}/{fileId}")

    public void getFile(HttpServletRequest request, HttpServletResponse response,
                        @PathVariable("userId")Integer userId,
                        @PathVariable("fileId") String fileId) throws IOException {
         fileInfoService.getFIle(request,response,fileId,userId);
    }


    @RequestMapping("/ts/getVideoInfo/{userId}/{fileId}")

    public void getVideoInfo(HttpServletRequest request, HttpServletResponse response,
                             @PathVariable("userId")Integer userId,
                             @PathVariable("fileId") String fileId) throws IOException {
       fileInfoService.getFIle(request, response, fileId,userId);
    }

    @RequestMapping("/createDownloadUrl/{userId}/{fileId}")

    public RestBean createDownloadUrl(@PathVariable("userId")  Integer userId,
                                        @PathVariable("fileId") String fileId) {
      return fileInfoService.createDownloadUrl(userId,fileId);
    }

    /**
     * 下载
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/download/{code}")

    public void download(HttpServletRequest request, HttpServletResponse response,
                         @PathVariable("code")  String code) throws Exception {
         fileInfoService.download(request,response,code);

    }


    @RequestMapping("/delFile")

    public RestBean delFile(@RequestBody AdminDeleteFileVo vo ) {
        String[] fileIdAndUserIdArray = vo.getFileIdAndUserIds().split(",");
        for (String fileIdAndUserId : fileIdAndUserIdArray) {
            String[] itemArray = fileIdAndUserId.split("_");
            fileInfoService.delFileBatch(Integer.valueOf(itemArray[0]), itemArray[1], true);
        }
        return RestBean.success(null);
    }


}
