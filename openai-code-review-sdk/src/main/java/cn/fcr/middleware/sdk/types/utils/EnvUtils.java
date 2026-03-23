package cn.fcr.middleware.sdk.types.utils;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 傅崇睿
 * @date 2026/03/20 15:36
 * @description EnvUtils
 */
public class EnvUtils {

    private static final Logger logger = LoggerFactory.getLogger(EnvUtils.class);

    private static final Map<String, CacheEntry> ENV_CACHE = new ConcurrentHashMap<>();

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

    private static final class CacheEntry {
        private static final CacheEntry ABSENT = new CacheEntry(null, false);

        private final String value;
        private final boolean present;

        private CacheEntry(String value, boolean present) {
            this.value = value;
            this.present = present;
        }

        private static CacheEntry of(String value) {
            return new CacheEntry(value, true);
        }
    }

    /**
     * 获取字符串类环境变量，优先级：系统环境变量 > 当前目录 .env > 父目录 .env。
     */
    public static String getEnv(String key) {
        if (key == null) {
            return null;
        }

        String normalizedKey = key.trim();
        if (normalizedKey.isEmpty()) {
            return null;
        }

        CacheEntry entry = ENV_CACHE.computeIfAbsent(normalizedKey, EnvUtils::resolveCachedEntry);
        return entry.present ? entry.value : null;
    }

    private static CacheEntry resolveCachedEntry(String key) {
        String value = normalizeValue(System.getenv(key));
        if (value != null) {
            return CacheEntry.of(value);
        }

        value = normalizeValue(DOTENV_CURRENT_DIR.get(key));
        if (value != null) {
            return CacheEntry.of(value);
        }

        value = normalizeValue(DOTENV_PARENT_DIR.get(key));
        if (value != null) {
            return CacheEntry.of(value);
        }

        return CacheEntry.ABSENT;
    }

    private static String normalizeValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    /**
     * 删除某个键的缓存，下次读取会重新解析环境变量。
     */
    public static void invalidate(String key) {
        if (key == null) {
            return;
        }
        String normalizedKey = key.trim();
        if (!normalizedKey.isEmpty()) {
            ENV_CACHE.remove(normalizedKey);
        }
    }

    /**
     * 清空所有缓存，常用于测试场景或运行时重新加载配置。
     */
    public static void clearCache() {
        ENV_CACHE.clear();
    }

}
