package cn.fcr.middleware.sdk.types.utils;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 傅崇睿
 * @date 2026/03/20 15:36
 * @description EnvUtils
 */
public class EnvUtils {

    private static final Logger logger = LoggerFactory.getLogger(EnvUtils.class);

    /**
     * 本地读 .env 文件；CI 环境（Actions）无 .env 时自动降级到系统环境变量（Secrets 注入方式）。
     */
    private static final Dotenv DOTENV_CURRENT_DIR = Dotenv.configure()
            .directory(".")
            .ignoreIfMissing()
            .load();
    private static final Dotenv DOTENV_PARENT_DIR = Dotenv.configure()
            .directory("..")
            .ignoreIfMissing()
            .load();

    private EnvUtils() {}

    /**
     * 获取字符串类环境变量，无默认值则返回 null (适合必须存在的 Key)
     */
    public static String getEnv(String key) {
        // 1. 系统获取
        String value = System.getenv(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        // 2. 尝试当前目录 .env
        value = DOTENV_CURRENT_DIR.get(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        // 3. 尝试上级目录 .env
        value = DOTENV_PARENT_DIR.get(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        return null;
    }

    public static String getEnv(String key, String defaultValue) {
        String value = getEnv(key);
        return (value == null) ? defaultValue : value;
    }

    /**
     * 获取整数类环境变量，带默认值
     */
    public static int getIntEnv(String key, int defaultValue) {
        String value = getEnv(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("环境变量 {} 非法：{}，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

}
