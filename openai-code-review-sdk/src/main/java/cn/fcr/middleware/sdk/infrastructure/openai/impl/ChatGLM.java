package cn.fcr.middleware.sdk.infrastructure.openai.impl;

import cn.fcr.middleware.sdk.infrastructure.config.AIConfig;
import cn.fcr.middleware.sdk.infrastructure.openai.DTO.ChatCompletionRequestDTO;
import cn.fcr.middleware.sdk.infrastructure.openai.DTO.ChatCompletionSyncResponseDTO;
import cn.fcr.middleware.sdk.infrastructure.openai.IOpenAI;
import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @author 傅崇睿
 * @date 2026/03/20 13:52
 * @description ChatGLM
 */
public class ChatGLM implements IOpenAI {


    private final AIConfig config;

    private static final Logger logger = LoggerFactory.getLogger(ChatGLM.class);

    public ChatGLM(AIConfig config) {
        this.config = config;
    }

    @Override
    public ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new IllegalArgumentException("requestDTO 不能为空");
        }

        String apiHost = requireNonBlank(config.getApiHost(), "AI_HOST");
        String apiKey = requireNonBlank(config.getApiKey(), "AI_KEY");
        int connectTimeout = config.getConnectTimeout();
        int readTimeout = config.getReadTimeout();
        int maxRetries = Math.max(1, config.getMaxRetries());
        int retryBackoff = Math.max(0, config.getRetryBackoff());

        String requestBody = JSON.toJSONString(requestDTO);
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = openConnection(apiHost, apiKey, connectTimeout, readTimeout);
                writeRequestBody(connection, requestBody);

                int responseCode = connection.getResponseCode();
                String responseBody = readBody(connection, responseCode);

                if (responseCode < 200 || responseCode >= 300) {
                    throw new RuntimeException("大模型请求失败，HTTP " + responseCode + "，响应：" + responseBody);
                }

                return parseResponse(responseBody);
            } catch (SocketTimeoutException e) {
                lastException = e;
                logger.warn("ChatGLM 请求超时，第 {}/{} 次重试，connectTimeout={}ms, readTimeout={}ms",
                        attempt, maxRetries, connectTimeout, readTimeout);
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep((long) retryBackoff * attempt);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试等待被中断", interruptedException);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("ChatGLM 网络调用失败：" + e.getMessage(), e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        throw new RuntimeException("ChatGLM 请求连续超时，请调整 AI_READ_TIMEOUT_MS 或缩小本次 diff 后重试", lastException);
    }

    private HttpURLConnection openConnection(String apiHost, String apiKey, int connectTimeout, int readTimeout) throws IOException {
        URL url = new URL(apiHost);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Java/11 OpenAiCodeReview");
        connection.setDoOutput(true);
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        return connection;
    }

    private void writeRequestBody(HttpURLConnection connection, String requestBody) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
            os.flush();
        }
    }

    private ChatCompletionSyncResponseDTO parseResponse(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new RuntimeException("大模型响应体为空");
        }

        ChatCompletionSyncResponseDTO response;
        try {
            response = JSON.parseObject(responseBody, ChatCompletionSyncResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("响应反序列化失败：" + responseBody, e);
        }

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()
                || response.getChoices().get(0) == null
                || response.getChoices().get(0).getMessage() == null) {
            logger.error("响应结构异常，原始响应：{}", responseBody);
            throw new RuntimeException("大模型响应结构异常：" + responseBody);
        }

        return response;
    }

    private String requireNonBlank(String value, String key) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("配置缺失：" + key);
        }
        return value;
    }

    /**
     * 读取响应体，无论成功或失败都读取，便于定位协议层问题。
     * @param connection HTTP连接对象
     * @param responseCode HTTP响应码
     * @return 响应体字符串
     */
    private String readBody(HttpURLConnection connection, int responseCode) throws IOException {
        InputStream stream = (responseCode >= 200 && responseCode < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();

        if (stream == null) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }
}
