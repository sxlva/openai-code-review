package cn.fcr.middleware.sdk.infrastructure.config;

/**
 * @author 傅崇睿
 * @date 2026/03/20 15:04
 * @description AIConfig
 */
public final class AIConfig {

    // 基础连接
    private final String apiHost;
    private final String apiKey;

    // 超时控制
    private final int connectTimeout;
    private final int readTimeout;

    // 容错策略
    private final int maxRetries;      // 最大重试次数
    private final int retryBackoff;    // 重试退避时间（毫秒）

    public AIConfig(String host, String apiKey, int connectTimeout, int readTimeout, int maxRetries, int retryBackoff) {
        this.apiHost = host;
        this.apiKey = apiKey;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.maxRetries = maxRetries;
        this.retryBackoff = retryBackoff;
    }

    public String getApiHost() {
        return apiHost;
    }

    public String getApiKey() {
        return apiKey;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getRetryBackoff() {
        return retryBackoff;
    }


}
