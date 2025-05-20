package com.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.constants.Constants;
import com.example.entity.Dto.FileInfo;
import com.example.entity.Dto.FileShare;
import com.example.entity.Enum.DateTimePatternEnum;
import com.example.entity.Enum.ResponseCodeEnum;
import com.example.entity.Enum.ShareValidTypeEnums;
import com.example.entity.Vo.request.FileShareQuery;
import com.example.entity.Vo.response.PaginationResultVO;
import com.example.exception.BusinessException;
import com.example.mapper.FileMapper;
import com.example.mapper.FileShareMapper;
import com.example.service.FileShareService;
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
    FileMapper fileMapper;

    @Override
    public PaginationResultVO findListByPage(FileShareQuery query) {

        // 1. 创建查询条件 - 只创建一次
        LambdaQueryWrapper<FileShare> queryWrapper = createQueryWrapper(query);

        // 2. 查询总记录数
        int count = (int) this.count(queryWrapper);

        // 3. 设置分页参数
        int pageNo = query.getPageNo() == null ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() == null ? 15 : query.getPageSize();
        Page<FileShare> page = new Page<>(pageNo, pageSize);

        // 4. 执行分页查询
        Page<FileShare> pageResult = this.page(page, queryWrapper);
        List<FileShare> list = pageResult.getRecords();

        // 4. 如果需要关联文件名信息，并且有查询结果
        if (Boolean.TRUE.equals(query.getQueryFileName()) && !list.isEmpty()) {
            // 提取所有文件ID
            List<String> fileIds = list.stream()
                    .map(FileShare::getFileId)
                    .filter(Objects::nonNull)  // 过滤掉null值
                    .distinct()  // 去重
                    .collect(Collectors.toList());

            if (!fileIds.isEmpty()) {
                // 查询文件信息
                LambdaQueryWrapper<FileInfo> fileQueryWrapper = new LambdaQueryWrapper<>();
                fileQueryWrapper.in(FileInfo::getFileId, fileIds)
                        .eq(FileInfo::getUserId, query.getUserId());  // 添加用户ID限制
                // 只选择需要的字段，提高性能
                fileQueryWrapper.select(
                        FileInfo::getFileId,
                        FileInfo::getFileName,
                        FileInfo::getFileType,
                        FileInfo::getFileCategory,
                        FileInfo::getFileCover,
                        FileInfo::getFolderType
                );

                List<FileInfo> fileInfoList = fileMapper.selectList(fileQueryWrapper);

                // 创建文件ID到文件信息的映射，便于快速查找
                Map<String, FileInfo> fileInfoMap = fileInfoList.stream()
                        .collect(Collectors.toMap(
                                FileInfo::getFileId,  // 键: 文件ID
                                Function.identity(),  // 值: 文件信息对象本身
                                (existing, replacement) -> existing  // 如有重复，保留第一个
                        ));

                // 为每个分享记录设置关联的文件信息
                for (FileShare share : list) {
                    if (share.getFileId() != null) {
                        FileInfo fileInfo = fileInfoMap.get(share.getFileId());
                        if (fileInfo != null) {
                            // 设置文件相关信息
                            share.setFileName(fileInfo.getFileName());
                            share.setFileType(fileInfo.getFileType());
                            share.setFileCategory(fileInfo.getFileCategory());
                            share.setFileCover(fileInfo.getFileCover());
                            share.setFolderType(fileInfo.getFolderType());
                        }
                    }
                }
            }
        }





        // 5. 构建分页结果对象 - 修正参数位置
        PaginationResultVO<FileShare> result = new PaginationResultVO(
                (int) pageResult.getTotal(),     // 总记录数
                (int) pageResult.getSize(),      // 页大小
                (int) pageResult.getCurrent(),   // 当前页码
                (int) pageResult.getPages(),     // 总页数
                list                             // 数据列表
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

    /**
     * 根据查询参数构建查询条件
     */
    private LambdaQueryWrapper<FileShare> createQueryWrapper(FileShareQuery query) {
        LambdaQueryWrapper<FileShare> queryWrapper = new LambdaQueryWrapper<>();

        if (query != null) {
            // 精确匹配条件
            if (query.getShareId() != null) {
                queryWrapper.eq(FileShare::getShareId, query.getShareId());
            }

            if (query.getFileId() != null) {
                queryWrapper.eq(FileShare::getFileId, query.getFileId());
            }

            if (query.getUserId() != null) {
                queryWrapper.eq(FileShare::getUserId, query.getUserId());
            }

            if (query.getValidType() != null) {
                queryWrapper.eq(FileShare::getValidType, query.getValidType());
            }

            if (query.getShowCount() != null) {
                queryWrapper.eq(FileShare::getShowCount, query.getShowCount());
            }

            if (query.getCode() != null) {
                queryWrapper.eq(FileShare::getCode, query.getCode());
            }

            // 模糊匹配条件
            if (query.getShareIdFuzzy() != null) {
                queryWrapper.like(FileShare::getShareId, query.getShareIdFuzzy());
            }

            if (query.getFileIdFuzzy() != null) {
                queryWrapper.like(FileShare::getFileId, query.getFileIdFuzzy());
            }

            if (query.getUserIdFuzzy() != null) {
                queryWrapper.like(FileShare::getUserId, query.getUserIdFuzzy());
            }

            if (query.getCodeFuzzy() != null) {
                queryWrapper.like(FileShare::getCode, query.getCodeFuzzy());
            }

            // 时间范围条件
            if (query.getExpireTimeStart() != null) {
                queryWrapper.ge(FileShare::getExpireTime, DateUtil.parse(query.getExpireTimeStart(),
                        DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
            }

            if (query.getExpireTimeEnd() != null) {
                queryWrapper.le(FileShare::getExpireTime, DateUtil.parse(query.getExpireTimeEnd(),
                        DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
            }

            if (query.getShareTimeStart() != null) {
                queryWrapper.ge(FileShare::getShareTime, DateUtil.parse(query.getShareTimeStart(),
                        DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
            }

            if (query.getShareTimeEnd() != null) {
                queryWrapper.le(FileShare::getShareTime, DateUtil.parse(query.getShareTimeEnd(),
                        DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
            }



            // 设置排序
            if (query.getOrderBy() != null && !query.getOrderBy().isEmpty()) {
                // 解析orderBy字符串，格式如"share_time desc"
                String[] orderByArr = query.getOrderBy().split(" ");
                String orderField = orderByArr[0];
                boolean isAsc = orderByArr.length <= 1 || !"desc".equalsIgnoreCase(orderByArr[1]);

                // 根据字段名设置排序
                switch (orderField) {
                    case "shareId":
                        queryWrapper.orderBy(true, isAsc, FileShare::getShareId);
                        break;
                    case "fileId":
                        queryWrapper.orderBy(true, isAsc, FileShare::getFileId);
                        break;
                    case "userId":
                        queryWrapper.orderBy(true, isAsc, FileShare::getUserId);
                        break;
                    case "validType":
                        queryWrapper.orderBy(true, isAsc, FileShare::getValidType);
                        break;
                    case "expireTime":
                        queryWrapper.orderBy(true, isAsc, FileShare::getExpireTime);
                        break;
                    case "shareTime":
                        queryWrapper.orderBy(true, isAsc, FileShare::getShareTime);
                        break;
                    case "code":
                        queryWrapper.orderBy(true, isAsc, FileShare::getCode);
                        break;
                    case "showCount":
                        queryWrapper.orderBy(true, isAsc, FileShare::getShowCount);
                        break;
                    case "fileName":
                        queryWrapper.orderBy(true, isAsc, FileShare::getFileName);
                        break;
                    case "folderType":
                        queryWrapper.orderBy(true, isAsc, FileShare::getFolderType);
                        break;
                    case "fileCategory":
                        queryWrapper.orderBy(true, isAsc, FileShare::getFileCategory);
                        break;
                    case "fileType":
                        queryWrapper.orderBy(true, isAsc, FileShare::getFileType);
                        break;
                    default:
                        // 默认按分享时间降序
                        queryWrapper.orderByDesc(FileShare::getShareTime);
                }
            } else {
                // 默认排序
                queryWrapper.orderByDesc(FileShare::getShareTime);
            }
        }

        return queryWrapper;
    }



}
