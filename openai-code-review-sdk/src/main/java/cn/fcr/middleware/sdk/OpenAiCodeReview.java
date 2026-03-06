package cn.fcr.middleware.sdk;

import cn.fcr.middleware.sdk.model.ChatCompletionRequest;
import cn.fcr.middleware.sdk.model.ChatCompletionSyncResponse;
import com.alibaba.fastjson2.JSON;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class OpenAiCodeReview {

    public static void main(String[] args) throws Exception {
        System.out.println("测试执行");

        // 1. 代码检出
        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD~1", "HEAD")
                .directory(new File("."));

        Process process = processBuilder.start();

        StringBuilder diffCode = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                diffCode.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Git 命令执行失败，退出码：" + exitCode);
        }

        String log = codeReview(diffCode.toString());
        System.out.println("code review：\n" + log);
    }

    private static String codeReview(String diffCode) throws Exception {
        String token = System.getenv("ZHIPU_AI_API_KEY");
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("环境变量 `ZHIPU_AI_API_KEY` 未设置");
        }

        URL url = new URL("https://open.bigmodel.cn/api/paas/v4/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + token.trim());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "Java/11 OpenAiCodeReview");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000); // 5秒连接不上就报错
        connection.setReadTimeout(90000);    // 90秒没返回结果就报错

        // 构建请求体
        List<ChatCompletionRequest.Prompt> prompts = new ArrayList<>();
        prompts.add(new ChatCompletionRequest.Prompt("system", "你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。"));
        prompts.add(new ChatCompletionRequest.Prompt("user", "代码如下:\n" + diffCode));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .messages(prompts)
                .build();

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = JSON.toJSONString(request).getBytes(StandardCharsets.UTF_8);
            os.write(input);
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        String responseBody = readBody(connection, responseCode);
        connection.disconnect();

        if (responseCode < 200 || responseCode >= 300) {
            throw new RuntimeException("大模型请求失败，HTTP " + responseCode + "，响应：" + responseBody);
        }

        ChatCompletionSyncResponse response = JSON.parseObject(responseBody, ChatCompletionSyncResponse.class);
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()
                || response.getChoices().get(0) == null
                || response.getChoices().get(0).getMessage() == null) {
            throw new RuntimeException("响应结构异常，无法解析评审内容：" + responseBody);
        }

        ChatCompletionSyncResponse.Message message = response.getChoices().get(0).getMessage();
        StringBuilder reviewBuilder = new StringBuilder();

        if (message.getContent() != null) {
            reviewBuilder.append(message.getContent());
        }
        if (message.getReasoning_content() != null && !message.getReasoning_content().isEmpty()) {
            if (reviewBuilder.length() > 0) {
                reviewBuilder.append("\n\n");
            }
            reviewBuilder.append("reasoning:\n").append(message.getReasoning_content());
        }

        return reviewBuilder.toString();
    }

    private static String readBody(HttpURLConnection connection, int responseCode) throws Exception {
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
