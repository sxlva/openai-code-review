package cn.fcr.middleware.sdk.infrastructure.openai;

import cn.fcr.middleware.sdk.infrastructure.config.AIConfig;
import cn.fcr.middleware.sdk.types.utils.EnvUtils;

/**
 * @author 傅崇睿
 * @date 2026/03/21 10:23
 * @description AIConfigFactory
 */
public class AIConfigFactory {

    public static AIConfig create() {
        // 1. 核心地址与密钥
        String host = EnvUtils.getEnv("CHATGLM_APIHOST");
        String apiKey = EnvUtils.getEnv("CHATGLM_APIKEYSECRET");

        if (null == apiKey || apiKey.isEmpty()) {
            throw new IllegalArgumentException("AI_KEY 缺失，请检查环境变量");
        }

        // 2. 超时配置
        int connectTimeout = EnvUtils.getIntEnv("AI_CONNECT_TIMEOUT_MS", 10000);
        int readTimeout = EnvUtils.getIntEnv("AI_READ_TIMEOUT_MS", 180000);

        // 3. 重试策略
        int maxRetries = EnvUtils.getIntEnv("AI_MAX_RETRIES", 2);
        int retryBackoff = EnvUtils.getIntEnv("AI_RETRY_BACKOFF_MS", 2000);

        return new AIConfig(host, apiKey, connectTimeout, readTimeout, maxRetries, retryBackoff);
    }

}
