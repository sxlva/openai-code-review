package cn.fcr.middleware.sdk.types.utils;

import com.alibaba.fastjson2.JSON;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author 傅崇睿
 * @date 2026/03/16 13:40
 * @description 具备缓存能力的微信 AccessToken 工具类
 */
@Slf4j
public class WXAccessTokenUtils {

    /**
     * 本地优先读取 .env 文件；CI 环境（GitHub Actions）无 .env 时自动降级到系统环境变量。
     * ignoreIfMissing() 保证文件不存在时不抛异常。
     */
    private static final Dotenv DOTENV_CURRENT_DIR = Dotenv.configure()
            .directory(".")
            .ignoreIfMissing()
            .load();
    private static final Dotenv DOTENV_PARENT_DIR = Dotenv.configure()
            .directory("..")
            .ignoreIfMissing()
            .load();

    private static final String APPID = resolveEnv("APP_ID");
    private static final String SECRET = resolveEnv("APP_SECRET");
    private static final String GRANT_TYPE = "client_credential";
    private static final String URL_TEMPLATE = "https://api.weixin.qq.com/cgi-bin/token?grant_type=%s&appid=%s&secret=%s";

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

    public static String getAccessToken() {
        try {
            String urlString = String.format(URL_TEMPLATE, GRANT_TYPE, APPID, SECRET);
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Print the response
                System.out.println("Response: " + response);

                Token token = JSON.parseObject(response.toString(), Token.class);

                return token.getAccess_token();
            } else {
                System.out.println("GET request failed");
                return null;
            }
        } catch (Exception e) {
            log.error("刷新微信AccessToken失败", e);
            return null;
        }
    }

    @Data
    public static class Token {
        private String access_token;
        private Integer expires_in;
    }

}
