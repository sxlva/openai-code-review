package cn.fcr.middleware.sdk;

import cn.fcr.middleware.sdk.domain.model.ChatCompletionRequest;
import cn.fcr.middleware.sdk.domain.model.ChatCompletionSyncResponse;
import cn.fcr.middleware.sdk.domain.model.Message;
import cn.fcr.middleware.sdk.types.utils.WXAccessTokenUtils;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class OpenAiCodeReview {

    private static final int AI_CONNECT_TIMEOUT_MS = getIntEnv("AI_CONNECT_TIMEOUT_MS", 10000);
    private static final int AI_READ_TIMEOUT_MS = getIntEnv("AI_READ_TIMEOUT_MS", 180000);
    private static final int AI_MAX_RETRIES = getIntEnv("AI_MAX_RETRIES", 2);
    private static final int AI_RETRY_BACKOFF_MS = getIntEnv("AI_RETRY_BACKOFF_MS", 2000);

    public static void main(String[] args) throws Exception {
        log.info("openai 代码评审，测试执行");
        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("环境变量 `GITHUB_TOKEN` 未设置");
        }

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

        // 2. chatglm 代码评审
        log.info("开始请求 ChatGLM 进行代码评审");
        String reviewResult = codeReview(diffCode.toString());
        log.info("code review： {}", reviewResult);

        // 3. 写入评审日志
        try {
            String logUrl = writeLog(token, reviewResult);
            log.info("评审日志写入成功，writeLog： {}", logUrl);

            // 4. 消息通知
            log.info("准备发送微信消息通知: {}", logUrl);
            pushMessage(logUrl);
        } catch (Exception e) {
            // 5. 打印异常
            log.error("代码评审后续处理流程发生异常", e);
        }

    }

    private static String codeReview(String diffCode) throws Exception {
        String token = System.getenv("ZHIPU_AI_API_KEY");
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("环境变量 `ZHIPU_AI_API_KEY` 未设置");
        }

        // 构建请求体
        List<ChatCompletionRequest.Prompt> prompts = new ArrayList<>();
        prompts.add(new ChatCompletionRequest.Prompt("system", "你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。"));
        prompts.add(new ChatCompletionRequest.Prompt("user", "代码如下:\n" + diffCode));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .messages(prompts)
                .build();
        String requestBody = JSON.toJSONString(request);

        Exception lastException = null;
        for (int attempt = 1; attempt <= AI_MAX_RETRIES; attempt++) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://open.bigmodel.cn/api/paas/v4/chat/completions");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + token.trim());
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "Java/11 OpenAiCodeReview");
                connection.setDoOutput(true);
                connection.setConnectTimeout(AI_CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(AI_READ_TIMEOUT_MS);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input);
                    os.flush();
                }

                int responseCode = connection.getResponseCode();
                // 获取 ai 回复消息体，无论成功还是失败都要读取，以便调试和日志记录
                String responseBody = readBody(connection, responseCode);

                if (responseCode < 200 || responseCode >= 300) {
                    throw new RuntimeException("大模型请求失败，HTTP " + responseCode + "，响应：" + responseBody);
                }

                // 解析响应体，提取评审内容
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
            } catch (SocketTimeoutException e) {
                lastException = e;
                log.warn("ChatGLM 请求超时，第 {}/{} 次重试，connectTimeout={}ms, readTimeout={}ms",
                        attempt, AI_MAX_RETRIES, AI_CONNECT_TIMEOUT_MS, AI_READ_TIMEOUT_MS);
                if (attempt < AI_MAX_RETRIES) {
                    Thread.sleep((long) AI_RETRY_BACKOFF_MS * attempt);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        throw new RuntimeException("ChatGLM 请求连续超时，请增大 AI_READ_TIMEOUT_MS 或缩小本次 diff 后重试", lastException);
    }

    private static int getIntEnv(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("环境变量 {} 非法：{}，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 读取响应体，无论是成功还是失败都要读取，以便调试和日志记录
     * @param connection HTTP连接对象
     * @param responseCode HTTP响应码
     * @return 响应体字符串
     * @throws Exception 读取响应体失败时抛出异常
     */
    private static String readBody(HttpURLConnection connection, int responseCode) throws Exception {
        InputStream stream = (responseCode >= 200 && responseCode < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();

        if (stream == null) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        // 字节转字符
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }

    private static String writeLog(String token, String reviewResult) throws Exception {
        try (Git git = Git.cloneRepository()
                .setURI("https://github.com/sxlva/openai-code-review-log.git")
                .setDirectory(new File("repo"))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                .call()) {

            // 1. 获取当天日期的文件夹，如果不存在就创建
            String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            File dateFolder = new File("repo/" + dateFolderName);
            if (!dateFolder.exists()) {
                dateFolder.mkdirs();
            }

            // 2. 写文件逻辑
            String fileName = generateRandomString(12) + ".md";
            File newFile = new File(dateFolder, fileName);
            try (FileWriter writer = new FileWriter(newFile)) {
                writer.write(reviewResult);
            }

            // 3. 执行 Git 操作
            git.add().addFilepattern(dateFolderName + "/" + fileName).call();
            git.commit().setMessage("Add new file via GitHub Actions").call();
            git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, "")).call();

            System.out.println("Changes have been pushed to the repository.");

            return "https://github.com/sxlva/openai-code-review-log/blob/master/" + dateFolderName + "/" + fileName;
        }

    }

    private static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }

    private static void pushMessage(String logUrl) {
        String accessToken = WXAccessTokenUtils.getAccessToken();
        System.out.println(accessToken);

        Message message = new Message();
        message.put("project", "big-market");
        message.put("review", logUrl);
        message.setUrl(logUrl);

        String url = String.format("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s", accessToken);
        sendPostRequest(url, JSON.toJSONString(message));
    }

    private static void sendPostRequest(String urlString, String jsonBody) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                String response = scanner.useDelimiter("\\A").next();
                System.out.println(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
