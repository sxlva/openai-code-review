package cn.fcr.middleware.sdk;

import cn.fcr.middleware.sdk.model.ChatCompletionRequest;
import cn.fcr.middleware.sdk.model.ChatCompletionSyncResponse;
import com.alibaba.fastjson2.JSON;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class OpenAiCodeReview {

    public static void main(String[] args) throws Exception {
        System.out.println("代码评审， 测试执行");

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

        String log = codeReview(diffCode.toString());
        System.out.println("code review：\n" + log);

        String logUrl = writeLog(token, log);
        System.out.println("writeLog：" + logUrl);
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
        // 获取ai回复消息体，无论成功还是失败都要读取，以便调试和日志记录
        String responseBody = readBody(connection, responseCode);
        connection.disconnect();

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

    private static String writeLog(String token, String log) throws Exception {
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
                writer.write(log);
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

}
