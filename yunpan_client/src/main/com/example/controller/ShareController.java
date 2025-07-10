package com.example.controller;


import com.example.constants.Const;
import com.example.entity.Dto.FileShare;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.FileShareQuery;
import com.example.entity.Vo.request.cancelShareVo;
import com.example.entity.Vo.request.loadShareVo;
import com.example.entity.Vo.request.shareFileVo;
import com.example.entity.Vo.response.PaginationResultVO;
import com.example.mapper.FileShareMapper;
import com.example.service.FileShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/share")
public class ShareController {
    @Autowired
    FileShareService fileShareService;

    @Autowired
    FileShareMapper fileShareMapper;

     @RequestMapping("/loadShareList")
    public RestBean loadShareList(@RequestAttribute(Const.ATTR_USER_ID)Integer userId, @RequestBody loadShareVo vo){


        vo.setUserId(userId);


          PaginationResultVO result= fileShareService.findListByPage(vo);

     return RestBean.success(result);
    }

    @RequestMapping("/shareFile")

    public RestBean shareFile(@RequestAttribute(Const.ATTR_USER_ID)Integer userId,
                              @RequestBody shareFileVo vo) {

        FileShare share = new FileShare();
        share.setFileId(vo.getFileId());
        share.setValidType(vo.getValidType());
        share.setCode(vo.getCode());
        share.setUserId(userId);
        fileShareService.saveShare(share);
        return RestBean.success(share);
    }

    @RequestMapping("/cancelShare")

    public RestBean cancelShare(@RequestAttribute(Const.ATTR_USER_ID)Integer userId,  @RequestBody cancelShareVo vo) {

        fileShareService.deleteFileShareBatch(vo.getShareIds().split(","), userId);

        return RestBean.success();
    }

}
