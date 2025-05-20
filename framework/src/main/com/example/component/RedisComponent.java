package com.example.component;

import com.alibaba.fastjson2.JSON;
import com.example.constants.Constants;
import com.example.entity.Dto.Account;
import com.example.entity.Dto.DownloadFileDto;
import com.example.entity.Dto.SysSettingsDto;
import com.example.entity.Dto.UserSpaceDTO;
import com.example.mapper.FileMapper;
import com.example.mapper.UserMapper;
import com.example.utils.RedisUtils;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis组件，用于缓存操作
 */
@Component
public class RedisComponent {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Resource
    private FileMapper fileMapper;
    
    @Resource
    private UserMapper userMapper;
     @Resource
    RedisUtils   redisUtils;
    
    /**
     * 缓存过期时间（1小时）
     */
    private static final Integer REDIS_KEY_EXPIRES_DAY = 1;
    
    /**
     * 获取用户空间使用情况
     * 先从Redis获取，如果没有则从数据库查询并缓存到Redis
     * @param userId 用户ID
     * @return 用户空间使用数据
     */
    public UserSpaceDTO getUserSpaceUse(Integer userId) {
        if (userId == null) {
            return null;
        }
        
        // 构建Redis键
        String redisKey = Constants.REDIS_KEY_USER_SPACE_USE + userId;
        
        // 从Redis获取数据
        UserSpaceDTO spaceDto = (UserSpaceDTO) redisUtils.get(redisKey);
        
        // 如果Redis中有数据，直接返回
        if (spaceDto != null) {
            return spaceDto;
        }
        
        // Redis中没有数据，从数据库查询
        spaceDto = new UserSpaceDTO();
        spaceDto.setId(userId);
        
        // 查询用户已使用空间
        Long useSpace = fileMapper.selectUserSpace(userId);
        spaceDto.setUseSpace(useSpace == null ? 0L : useSpace);
        
        // 查询用户总空间
        Account userInfo = userMapper.findById(userId);
        if (userInfo != null && userInfo.getTotalSpace() != null) {
            spaceDto.setTotalSpace(userInfo.getTotalSpace());
        } else {
        // 如果用户没有设置总空间，使用默认值10GB
            spaceDto.setTotalSpace(10L * 1024 * 1024 * 1024); // 10GB
        }
        
        // 将数据缓存到Redis，设置过期时间
        redisUtils.setex(redisKey, spaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
        
        return spaceDto;
    }
    
    /**
     * 刷新用户空间使用情况缓存
     * @param userId 用户ID
     */
    public void refreshUserSpaceUse(Integer userId) {
        if (userId == null) {
            return;
        }
        
        // 删除缓存，下次获取时会重新计算
        String redisKey = Constants.REDIS_KEY_USER_SPACE_USE + userId;
        redisUtils.delete(redisKey);
    }

    /**
     * 保存用户空间使用情况到Redis
     * @param userId 用户ID
     * @param userSpaceDto 用户空间数据
     */
    public void saveUserSpaceUse(Integer userId, UserSpaceDTO userSpaceDto) {
        if (userId == null || userSpaceDto == null) {
            return;
        }
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, userSpaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
    }

    public Long getFileTempSize(Integer userId, String fileId) {
        Long currentSize = getFileSizeFromRedis(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId);
        return currentSize;
    }
    //保存文件临时大小
    public void saveFileTempSize(Integer userId, String fileId, Long fileSize) {
        Long currentSize = getFileTempSize(userId, fileId);
        redisUtils.setex(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId, currentSize + fileSize, Constants.REDIS_KEY_EXPIRES_ONE_HOUR);
    }

    private Long getFileSizeFromRedis(String key) {
        Object sizeObj = redisUtils.get(key);
        if (sizeObj == null) {
            return 0L;
        }
        if (sizeObj instanceof Integer) {
            return ((Integer) sizeObj).longValue();
        } else if (sizeObj instanceof Long) {
            return (Long) sizeObj;
        }

        return 0L;
    }
    public void saveDownloadCode(String code, DownloadFileDto downloadFileDto) {
        redisUtils.setex(Constants.REDIS_KEY_DOWNLOAD + code, downloadFileDto, Constants.REDIS_KEY_EXPIRES_FIVE_MIN);
    }

    public DownloadFileDto getDownloadCode(String code) {
        return (DownloadFileDto) redisUtils.get(Constants.REDIS_KEY_DOWNLOAD + code);
    }
    /**
     * 获取系统设置
     *
     * @return
     */
    public SysSettingsDto getSysSettingsDto() {
        SysSettingsDto sysSettingsDto = (SysSettingsDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingsDto == null) {
            sysSettingsDto = new SysSettingsDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
        }
        return sysSettingsDto;
    }

    /**
     * 重置用户空间使用情况
     * 从数据库重新计算并更新缓存
     * @param userId 用户ID
     * @return 最新的用户空间数据
     */
    public UserSpaceDTO resetUserSpaceUse(Integer userId) {
        if (userId == null) {
            return null;
        }
        
        UserSpaceDTO spaceDto = new UserSpaceDTO();
        spaceDto.setId(userId);
        
        // 重新计算已使用空间
        Long useSpace = fileMapper.selectUserSpace(userId);
        spaceDto.setUseSpace(useSpace == null ? 0L : useSpace);

        // 获取总空间
        Account userInfo = userMapper.findById(userId);
        if (userInfo != null && userInfo.getTotalSpace() != null) {
            spaceDto.setTotalSpace(userInfo.getTotalSpace());
        } else {
            // 默认10GB
            spaceDto.setTotalSpace(10L * 1024 * 1024 * 1024);
        }
        
        // 更新缓存
        saveUserSpaceUse(userId, spaceDto);
        return spaceDto;
    }
} 