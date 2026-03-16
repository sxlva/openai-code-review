package cn.fcr.middleware.sdk.test;

import cn.fcr.middleware.sdk.types.utils.WXAccessTokenUtils;
import com.alibaba.fastjson2.JSON;
import com.google.common.collect.ImmutableMap;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Data;
import org.junit.Test;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * @author 傅崇睿
 * @date 2026/03/05 21:05
 * @description ApiTest
 */
public class ApiTest {

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

    private static String resolveEnv(String key) {
        String value = System.getenv(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        value = DOTENV_CURRENT_DIR.get(key, null);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        value = DOTENV_PARENT_DIR.get(key, null);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        return null;
    }

    public static void main(String[] args) {

    }

    /**
     * 微信信息test
     */
    @Test
    public void test_wx() {
        String accessToken = WXAccessTokenUtils.getAccessToken();
        System.out.println(accessToken);

        Message message = new Message();
        message.put("project","big-market");
        message.put("review","feat: 新加功能");

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

    @Data
    public static class Message {
        /** 接收方 openid，本地从 .env 读取，CI 从 Actions Secrets 注入 */
        private String touser = resolveEnv("TOUSER");
        /** 模板 ID，本地从 .env 读取，CI 从 Actions Variables 注入 */
        private String template_id = resolveEnv("TEMPLATE_ID");
        private String url = "https://github.com/sxlva/openai-code-review-log";
        private Map<String, Map<String, String>> data = new HashMap<>();

        public void put(String key, String value) {
            data.put(key, ImmutableMap.of("value", value));
        }

    }

}
