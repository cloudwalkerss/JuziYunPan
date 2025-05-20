package com.example.utils;



import com.example.constants.Constants;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class StringTools {

    public static String encodeByMD5(String originString) {
        return StringTools.isEmpty(originString) ? null : DigestUtils.md5Hex(originString);
    }

    public static boolean isEmpty(String str) {

        if (null == str || "".equals(str) || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else if ("".equals(str.trim())) {
            return true;
        }
        return false;
    }

    public static String getFileSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        String suffix = fileName.substring(index);
        return suffix;
    }

    /**
     * 获取文件后缀名（不带点）
     */
    public static String getFileSuffixWithoutDot(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        String suffix = fileName.substring(index + 1);
        return suffix;
    }

    /**
     * 检查文件类型是否允许上传
     * @param fileName 文件名
     * @return 是否允许
     */
    public static boolean isAllowFileType(String fileName) {
        String fileSuffix = getFileSuffixWithoutDot(fileName).toLowerCase();
        return Arrays.asList(Constants.ALLOW_FILE_TYPES).contains(fileSuffix);
    }

    /**
     * 格式化文件大小
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小
     */
    public static String formatFileSize(Long size) {
        if (size == null || size <= 0) {
            return "0 B";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB", "PB", "EB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public static String getFileNameNoSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return fileName;
        }
        fileName = fileName.substring(0, index);
        return fileName;
    }

    public static String rename(String fileName) {
        String fileNameReal = getFileNameNoSuffix(fileName);
        String suffix = getFileSuffix(fileName);
        return fileNameReal + "_" + getRandomString(Constants.LENGTH_5) + suffix;
    }

    public static final String getRandomString(Integer count) {
        return RandomStringUtils.random(count, true, true);
    }

    public static final String getRandomNumber(Integer count) {
        return RandomStringUtils.random(count, false, true);
    }


    public static String escapeTitle(String content) {
        if (isEmpty(content)) {
            return content;
        }
        content = content.replace("<", "&lt;");
        return content;
    }


    public static String escapeHtml(String content) {
        if (isEmpty(content)) {
            return content;
        }
        content = content.replace("<", "&lt;");
        content = content.replace(" ", "&nbsp;");
        content = content.replace("\n", "<br>");
        return content;
    }

    public static boolean pathIsOk(String path) {
        if (StringTools.isEmpty(path)) {
            return true;
        }
        if (path.contains("../") || path.contains("..\\")) {
            return false;
        }
        return true;
    }
}
