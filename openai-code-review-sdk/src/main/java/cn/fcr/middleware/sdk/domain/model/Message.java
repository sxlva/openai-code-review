package cn.fcr.middleware.sdk.domain.model;

import com.google.common.collect.ImmutableMap;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信模板消息模型。
 * <p>
 * 用于组装模板消息请求体，最终序列化为平台接口所需 JSON 结构。
 */
@Data
public class Message {

    /** 接收方用户标识（openid）。 */
    private String touser = System.getenv("TOUSER");
    /** 模板 ID */
    private String template_id = System.getenv("TEMPLATE_ID");
    /** 模板点击跳转链接。 */
    private String url = "https://github.com/sxlva/openai-code-review-log";
    /** 模板字段数据，结构为 field -> { value: xxx }。 */
    private Map<String, Map<String, String>> data = new HashMap<>();

    /**
     * 写入一个模板变量。
     *
     * @param key 模板字段名
     * @param value 字段值
     */
    public void put(String key, String value) {
        data.put(key, ImmutableMap.of("value", value));
    }


}
