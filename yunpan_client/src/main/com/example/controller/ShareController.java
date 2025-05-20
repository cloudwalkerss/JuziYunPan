package com.example.controller;


import com.example.constants.Const;
import com.example.entity.Dto.FileShare;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.FileShareQuery;
import com.example.entity.Vo.response.PaginationResultVO;
import com.example.mapper.FileShareMapper;
import com.example.service.FileShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestAttribute;
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
    public RestBean loadShareList(@RequestAttribute(Const.ATTR_USER_ID)Integer userId, FileShareQuery query){


        query.setUserId(userId);
         query.setQueryFileName(true);
          PaginationResultVO result= fileShareService.findListByPage(query);

     return RestBean.success(result);
    }

    @RequestMapping("/shareFile")

    public RestBean shareFile(@RequestAttribute(Const.ATTR_USER_ID)Integer userId,
                                 String fileId,
                                Integer validType,
                                String code) {

        FileShare share = new FileShare();
        share.setFileId(fileId);
        share.setValidType(validType);
        share.setCode(code);
        share.setUserId(userId);
        fileShareService.saveShare(share);
        return RestBean.success(share);
    }

    @RequestMapping("/cancelShare")

    public RestBean cancelShare(@RequestAttribute(Const.ATTR_USER_ID)Integer userId,  String shareIds) {

        fileShareService.deleteFileShareBatch(shareIds.split(","), userId);

        return RestBean.success();
    }

}
