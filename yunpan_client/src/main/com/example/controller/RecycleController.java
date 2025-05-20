package com.example.controller;

import com.example.constants.Const;
import com.example.entity.Enum.FileDelFlagEnums;
import com.example.entity.RestBean;
import com.example.entity.Vo.request.FileInfoQuery;
import com.example.entity.Vo.response.FileInfoVO;
import com.example.entity.Vo.response.PaginationResultVO;
import com.example.service.FileService;
import com.example.utils.BeanCopyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recycle")
public class RecycleController {
     @Autowired
    private FileService fileService;

     @Autowired
    BeanCopyUtils beanCopyUtils;
    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadRecycleList")

    public RestBean loadRecycleList(@RequestAttribute(Const.ATTR_USER_ID)Integer userId, Integer pageNo, Integer pageSize) {
        FileInfoQuery query = new FileInfoQuery();
        query.setPageSize(pageSize);
        query.setPageNo(pageNo);
        query.setUserId(userId);
        query.setOrderBy("recovery_time desc");
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());

        PaginationResultVO<FileInfoVO> result = fileService.findListByPage(query);


        return RestBean.success(result);
    }

    @RequestMapping("/recoverFile")
    //恢复删除的文件
    public RestBean recoverFile(@RequestAttribute(Const.ATTR_USER_ID)Integer userId, String fileIds) {
        return fileService.recoverFileBatch(userId, fileIds);
    }

    @RequestMapping("/delFile")
    public RestBean delFile(@RequestAttribute(Const.ATTR_USER_ID)Integer userId, String fileIds) {
        return fileService.delFileBatch(userId, fileIds, false);
    }
}
