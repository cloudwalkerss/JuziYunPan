package com.example.constants;

public class Constants {
    public static final String ZERO_STR = "0";

    public static final Integer ZERO = 0;

    public static final Integer ONE = 1;

    public static final Integer LENGTH_30 = 30;

    public static final Integer LENGTH_10 = 10;

    public static final Integer LENGTH_20 = 20;

    public static final Integer LENGTH_5 = 5;

    public static final Integer LENGTH_15 = 15;

    public static final Integer LENGTH_150 = 150;

    public static final Integer LENGTH_50 = 50;

    public static final String SESSION_KEY = "session_key";

    public static final String SESSION_SHARE_KEY = "session_share_key_";

    public static final String FILE_FOLDER_FILE = "/file/";

    public static final String FILE_FOLDER_TEMP = "/temp/";

    public static final String IMAGE_PNG_SUFFIX = ".png";

    public static final String TS_NAME = "index.ts";

    public static final String M3U8_NAME = "index.m3u8";

    public static final String CHECK_CODE_KEY = "check_code_key";

    public static final String CHECK_CODE_KEY_EMAIL = "check_code_key_email";

    public static final String AVATAR_SUFFIX = ".jpg";

    public static final String FILE_FOLDER_AVATAR_NAME = "avatar/";

    public static final String AVATAR_DEFUALT = "default_avatar.jpg";

    public static final String VIEW_OBJ_RESULT_KEY = "result";

    /**
     * 文件上传相关常量
     */
    // 默认分片大小 5MB
    public static final Long CHUNK_SIZE = 5 * 1024 * 1024L;
    
    // 单文件最大大小 10GB
    public static final Long MAX_FILE_SIZE = 10 * 1024 * 1024 * 1024L;
    
    // 上传临时文件保存时间 (秒)
    public static final Integer UPLOAD_TEMP_EXPIRE_TIME = 60 * 60 * 24;
    
    /**
     * 文件类型限制
     */
    public static final String[] ALLOW_FILE_TYPES = new String[]{
            // 图片
            "jpg", "jpeg", "png", "gif", "bmp", "webp",
            // 文档
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "md",
            // 音视频
            "mp3", "mp4", "avi", "rmvb", "flv", "wmv", "mkv",
            // 压缩文件
            "zip", "rar", "7z", "tar", "gz",
            // 其他
            "apk", "exe", "csv", "json", "xml"
    };

    /**
     * redis key 相关
     */

    /**
     * 过期时间 1分钟
     */
    public static final Integer REDIS_KEY_EXPIRES_ONE_MIN = 60;

    /**
     * 过期时间 1天
     */
    public static final Integer REDIS_KEY_EXPIRES_DAY = REDIS_KEY_EXPIRES_ONE_MIN * 60 * 24;

    public static final Integer REDIS_KEY_EXPIRES_ONE_HOUR = REDIS_KEY_EXPIRES_ONE_MIN * 60;

    public static final Long MB = 1024 * 1024L;

    /**
     * 过期时间5分钟
     */
    public static final Integer REDIS_KEY_EXPIRES_FIVE_MIN = REDIS_KEY_EXPIRES_ONE_MIN * 5;


    public static final String REDIS_KEY_DOWNLOAD = "easypan:download:";

    public static final String REDIS_KEY_SYS_SETTING = "easypan:syssetting:";

    public static final String REDIS_KEY_USER_SPACE_USE = "easypan:user:spaceuse:";

    public static final String REDIS_KEY_USER_FILE_TEMP_SIZE = "easypan:user:file:temp:";

}
