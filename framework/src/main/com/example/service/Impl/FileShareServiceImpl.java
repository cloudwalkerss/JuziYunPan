package com.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.constants.Constants;
import com.example.entity.Dto.Account;
import com.example.entity.Dto.FileInfo;
import com.example.entity.Dto.FileShare;
import com.example.entity.Enum.DateTimePatternEnum;
import com.example.entity.Enum.ResponseCodeEnum;
import com.example.entity.Enum.ShareValidTypeEnums;
import com.example.entity.Vo.request.AdminShareQuery;
import com.example.entity.Vo.request.FileShareQuery;
import com.example.entity.Vo.request.loadShareVo;
import com.example.entity.Vo.response.PaginationResultVO;
import com.example.entity.Vo.response.ShareInfoVO;
import com.example.entity.Vo.response.responseShareVo;
import com.example.exception.BusinessException;
import com.example.mapper.FileMapper;
import com.example.mapper.FileShareMapper;
import com.example.mapper.UserMapper;
import com.example.service.FileShareService;
import com.example.utils.BeanCopyUtils;
import com.example.utils.DateUtil;
import com.example.utils.StringTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare>implements FileShareService {
    @Autowired
    private FileShareMapper fileShareMapper;
    @Autowired
    BeanCopyUtils beanCopyUtils;
    @Autowired
    FileMapper fileMapper;
    @Autowired
    UserMapper userMapper;

    @Override
    public PaginationResultVO<FileShare> findListByPage(loadShareVo vo) {

        // 1. 设置分页参数
        int pageNo = vo.getPageNo() == null ? 1 : vo.getPageNo();
        int pageSize = vo.getPageSize() == null ? 15 : vo.getPageSize();

        // 2. 构造分页对象
        Page<FileShare> page = new Page<>(pageNo, pageSize);

        // 3. 构造查询条件（使用构造函数方式传入实体）
        FileShare fileShare = new FileShare();
        fileShare.setUserId(vo.getUserId());
        LambdaQueryWrapper<FileShare> queryWrapper = new LambdaQueryWrapper<>(fileShare);

        // 4. 执行分页查询
        Page<FileShare> fileSharePage = fileShareMapper.selectPage(page, queryWrapper);

        List<FileShare> list = fileSharePage.getRecords();
        for (FileShare share : list) {
            FileInfo fileInfo=fileMapper.selectByFileIdAndUserId(share.getFileId(),share.getUserId());
            share.setFileName(fileInfo.getFileName());
            share.setFileType(fileInfo.getFileType());
            share.setFileCover(fileInfo.getFileCover());
            share.setFileCategory(fileShare.getFileCategory());
            share.setFolderType(fileShare.getFolderType());

        }
//        // 5. 复制结果列表到 VO 列表
//        List<responseShareVo> responseShareVoList = beanCopyUtils.copyBeanList(fileSharePage.getRecords(), responseShareVo.class);
//
//        for (responseShareVo responseShareVo : responseShareVoList) {
//             String fileId=responseShareVo.getFileId();
//             FileInfo fileInfo=fileMapper.selectById(fileId);
//             responseShareVo.setFileName(fileInfo.getFileName());
//
//        }
        // 6. 构造分页结果对象
        PaginationResultVO<FileShare> result = new PaginationResultVO<>(
                (int) fileSharePage.getTotal(),     // 总记录数
                (int) fileSharePage.getSize(),      // 页大小
                (int) fileSharePage.getCurrent(),   // 当前页码
                (int) fileSharePage.getPages(),     // 总页数
                list                 // 数据列表
        );

        return result;
    }


    @Override
    public void saveShare(FileShare share) {
        ShareValidTypeEnums typeEnums=ShareValidTypeEnums.getByType(share.getValidType());
        if(typeEnums==null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if(ShareValidTypeEnums.FOREVER!=typeEnums){
            share.setExpireTime(DateUtil.getAfterDate(typeEnums.getDays()));
        }
        Date currentDate = new Date();
        share.setShareTime(currentDate);
        if(StringTools.isEmpty(share.getCode())){
            share.setCode(StringTools.getRandomString(Constants.LENGTH_5));

        }
        share.setShareId(StringTools.getRandomString(Constants.LENGTH_20));
        this.save(share);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileShareBatch(String[] split, Integer userId) {
        UpdateWrapper<FileShare> Wrapper = new UpdateWrapper<>();
        Wrapper.eq("user_id", userId)
                .in("share_id", split);
     Integer count= baseMapper.delete(Wrapper);
     if(count!=split.length){
         throw new BusinessException(ResponseCodeEnum.CODE_600);
     }


    }

    @Override
    public FileShare getFileShareByShareId(String shareId) {

        QueryWrapper<FileShare> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("share_id", shareId);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public void updateShareCount(String shareId) {
        // 使用UpdateWrapper更新浏览次数
        UpdateWrapper<FileShare> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("share_id", shareId);
        updateWrapper.setSql("show_count = show_count + 1");
        this.update(updateWrapper);
    }

    @Override
    public PaginationResultVO<FileShare> findShareListForAdmin(AdminShareQuery query) {
        // 1. 设置分页参数
        int pageNo = query.getPageNo() == null ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() == null ? 15 : query.getPageSize();

        // 2. 构造分页对象
        Page<FileShare> page = new Page<>(pageNo, pageSize);

        // 3. 构造查询条件
        QueryWrapper<FileShare> queryWrapper = new QueryWrapper<>();
        
        // 设置排序
        if (StringTools.isEmpty(query.getOrderBy())) {
            queryWrapper.orderByDesc("share_time");
        } else {
            queryWrapper.last("order by " + query.getOrderBy());
        }
        
        // 根据关键词搜索（文件名或用户名）
        if (!StringTools.isEmpty(query.getKeyword())) {
            queryWrapper.inSql("file_id", "select file_id from file_info where file_name like '%" + query.getKeyword() + "%'")
                    .or()
                    .inSql("user_id", "select id from user_info where username like '%" + query.getKeyword() + "%' or nickname like '%" + query.getKeyword() + "%'");
        }
        
        // 根据用户ID筛选
        if (query.getUserId() != null) {
            queryWrapper.eq("user_id", query.getUserId());
        }
        
        // 根据有效期类型筛选
        if (query.getValidType() != null) {
            queryWrapper.eq("valid_type", query.getValidType());
        }
        
        // 4. 执行分页查询
        Page<FileShare> fileSharePage = fileShareMapper.selectPage(page, queryWrapper);
        List<FileShare> list = fileSharePage.getRecords();
        
        // 5. 补充文件信息和用户信息
        for (FileShare share : list) {
            // 获取文件信息
            FileInfo fileInfo = fileMapper.selectByFileIdAndUserId(share.getFileId(),share.getUserId());
            if (fileInfo != null) {

                share.setFileName(fileInfo.getFileName());
                share.setFileType(fileInfo.getFileType());
                share.setFileCover(fileInfo.getFileCover());
                share.setFileCategory(fileInfo.getFileCategory());
                share.setFolderType(fileInfo.getFolderType());
            }
            
            // 获取用户信息
            Account userInfo = userMapper.selectById(share.getUserId());
            if (userInfo != null) {
                // 设置用户名
                share.setNickName(userInfo.getNickname());
            }
        }
        
        // 6. 构造分页结果对象
        PaginationResultVO<FileShare> result = new PaginationResultVO<>(
                (int) fileSharePage.getTotal(),     // 总记录数
                (int) fileSharePage.getSize(),      // 页大小
                (int) fileSharePage.getCurrent(),   // 当前页码
                (int) fileSharePage.getPages(),     // 总页数
                list                                // 数据列表
        );
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileShareByAdmin(String shareId) {
        if (StringTools.isEmpty(shareId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        
        // 管理员直接删除分享，不需要验证用户ID
        QueryWrapper<FileShare> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("share_id", shareId);
        
        int count = baseMapper.delete(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getMsg());
        }
    }

    @Override
    public ShareInfoVO getShareDetailForAdmin(String shareId) {
        // 1. 获取分享信息
        FileShare share = this.getFileShareByShareId(shareId);
        if (share == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        
        // 2. 转换为VO对象
        ShareInfoVO shareInfoVO = beanCopyUtils.copyBean(share, ShareInfoVO.class);
        
        // 3. 获取文件信息
        FileInfo fileInfo = fileMapper.selectById(share.getFileId());
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        
        shareInfoVO.setFileName(fileInfo.getFileName());
        
        // 4. 获取用户信息
        Account userInfo = userMapper.selectById(share.getUserId());
        if (userInfo != null) {
            shareInfoVO.setNickName(userInfo.getNickname());
            shareInfoVO.setAvatar(userInfo.getAvatar());
            shareInfoVO.setUserId(userInfo.getId());
        }
        
        return shareInfoVO;
    }
}
