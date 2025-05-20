package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Dto.FileInfo;
import com.example.entity.Vo.request.FileInfoQuery;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 文件信息数据库操作接口
 */
@Mapper
public interface FileMapper extends BaseMapper<FileInfo> {
    
    /**
     * 根据条件查询文件列表
     */
    @Select("<script>" +
            "SELECT file_id, user_id, file_md5, file_pid, file_size, file_name, file_cover, file_path, " +
            "create_time, update_time, folder_type, file_category, file_type, status, recovery_time, del_flag " +
            "FROM file_info" +
            "<where>" +
            "<if test='userId != null'> AND user_id = #{userId}</if>" +
            "<if test='fileId != null and fileId != \"\"'> AND file_id = #{fileId}</if>" +
            "<if test='filePid != null and filePid != \"\"'> AND file_pid = #{filePid}</if>" +
            "<if test='fileName != null and fileName != \"\"'> AND file_name LIKE CONCAT('%', #{fileName}, '%')</if>" +
            "<if test='fileCategory != null'> AND file_category = #{fileCategory}</if>" +
            "<if test='folderType != null'> AND folder_type = #{folderType}</if>" +
            "<if test='fileMd5 != null and fileMd5 != \"\"'> AND file_md5 = #{fileMd5} AND folder_type = 0</if>" +
            "<if test='delFlag != null'> AND del_flag = #{delFlag}</if>" +
            "<if test='status != null'> AND status = #{status}</if>" +
            "</where>" +
            "<if test='orderBy != null and orderBy != \"\"'> ORDER BY ${orderBy}</if>" +
            "</script>")
    List<FileInfo> selectFileList(FileInfoQuery fileInfo);
    
    /**
     * 根据文件ID和用户ID查询文件
     */
    @Select("SELECT * FROM file_info WHERE file_id = #{fileId} AND user_id = #{userId}")
    FileInfo selectByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") Integer userId);
    
    /**
     * 插入文件记录
     */
    @Insert("INSERT INTO file_info(file_id, user_id, file_md5, file_pid, file_size, file_name, file_cover, file_path, " +
            "create_time, update_time, folder_type, file_category, file_type, status, recovery_time, del_flag) " +
            "VALUES (#{fileId}, #{userId}, #{fileMd5}, #{filePid}, #{fileSize}, #{fileName}, #{fileCover}, #{filePath}, " +
            "#{createTime}, #{updateTime}, #{folderType}, #{fileCategory}, #{fileType}, #{status}, #{recoveryTime}, #{delFlag})")
    int insert(FileInfo fileInfo);
    
    /**
     * 查询用户已使用空间
     */
    @Select("SELECT IFNULL(SUM(file_size), 0) FROM file_info WHERE user_id = #{userId} AND del_flag = 2")
    Long selectUserSpace(@Param("userId") Integer userId);
    
    /**
     * 根据文件ID和用户ID以及旧状态更新文件状态
     */
    @Update("<script>" +
            "UPDATE file_info" +
            "<set>" +
            "<if test='bean.fileMd5 != null'>file_md5 = #{bean.fileMd5},</if>" +
            "<if test='bean.filePid != null'>file_pid = #{bean.filePid},</if>" +
            "<if test='bean.fileSize != null'>file_size = #{bean.fileSize},</if>" +
            "<if test='bean.fileName != null'>file_name = #{bean.fileName},</if>" +
            "<if test='bean.fileCover != null'>file_cover = #{bean.fileCover},</if>" +
            "<if test='bean.filePath != null'>file_path = #{bean.filePath},</if>" +
            "<if test='bean.createTime != null'>create_time = #{bean.createTime},</if>" +
            "<if test='bean.updateTime != null'>update_time = #{bean.updateTime},</if>" +
            "<if test='bean.folderType != null'>folder_type = #{bean.folderType},</if>" +
            "<if test='bean.fileCategory != null'>file_category = #{bean.fileCategory},</if>" +
            "<if test='bean.fileType != null'>file_type = #{bean.fileType},</if>" +
            "<if test='bean.status != null'>status = #{bean.status},</if>" +
            "<if test='bean.recoveryTime != null'>recovery_time = #{bean.recoveryTime},</if>" +
            "<if test='bean.delFlag != null'>del_flag = #{bean.delFlag},</if>" +
            "</set>" +
            "WHERE file_id = #{fileId} AND user_id = #{userId} AND status = #{oldStatus}" +
            "</script>")
    void updateFileStatusWithOldStatus(@Param("fileId") String fileId, @Param("userId") Integer userId, 
                                      @Param("bean") FileInfo bean, @Param("oldStatus") Integer oldStatus);


} 