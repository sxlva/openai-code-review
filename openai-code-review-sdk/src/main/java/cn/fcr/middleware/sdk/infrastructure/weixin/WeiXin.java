package cn.fcr.middleware.sdk.infrastructure.weixin;

import cn.fcr.middleware.sdk.infrastructure.config.WeiXinConfig;
import cn.fcr.middleware.sdk.infrastructure.weixin.DTO.TemplateMessageDTO;
import cn.fcr.middleware.sdk.types.utils.WXAccessTokenUtils;
import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

/**
 * @author 傅崇睿
 * @date 2026/03/20 13:55
 * @description WeiXin
 */
public class WeiXin {

    private final Logger logger = LoggerFactory.getLogger(WeiXin.class);

    private final WeiXinConfig weiXinConfig;

    public WeiXin(WeiXinConfig weiXinConfig) {
        this.weiXinConfig = weiXinConfig;
    }

    public void sendTemplateMessage(String logUrl, Map<String, Map<String, String>> data) throws Exception {
        String accessToken = WXAccessTokenUtils.getAccessToken(weiXinConfig.getAppId(), weiXinConfig.getSecret());

        TemplateMessageDTO templateMessageDTO = new TemplateMessageDTO(weiXinConfig.getToUser(), weiXinConfig.getTemplateId());
        templateMessageDTO.setUrl(logUrl);
        templateMessageDTO.setData(data);

        URL url = new URL(String.format("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=%s", accessToken));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = JSON.toJSONString(templateMessageDTO).getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
            String response = scanner.useDelimiter("\\A").next();
            logger.info("openai-code-review weixin template message! {}", response);
        }
    }
}
