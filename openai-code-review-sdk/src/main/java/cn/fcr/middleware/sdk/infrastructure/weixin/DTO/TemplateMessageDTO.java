package cn.fcr.middleware.sdk.infrastructure.weixin.DTO;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信模板消息模型。
 * <p>
 * 用于组装模板消息请求体，最终序列化为平台接口所需 JSON 结构。
 */
public class TemplateMessageDTO {

    /** 接收方用户标识（openid）。 */
    private String touser;
    /** 模板 ID */
    private String template_id;
    /** 模板点击跳转链接。 */
    private String url;
    /** 模板字段数据，结构为 field -> { value: xxx }。 */
    private Map<String, Map<String, String>> data = new HashMap<>();

    public TemplateMessageDTO(String touser, String template_id) {
        this.touser = touser;
        this.template_id = template_id;
    }


    /**
     * 写入一个模板变量。
     *
     * @param key 模板字段名
     * @param value 字段值
     */
    public void put(String key, String value) {
        data.put(key, ImmutableMap.of("value", value));
    }

    public static void put(Map<String, Map<String, String>> data, TemplateKey key, String value) {
        data.put(key.getCode(), ImmutableMap.of("value", value));
    }

    public enum TemplateKey {
        REPO_NAME("repo_name","项目名称"),
        BRANCH_NAME("branch_name","分支名称"),
        COMMIT_AUTHOR("commit_author","提交者"),
        COMMIT_MESSAGE("commit_message","提交信息"),
        ;

        private String code;
        private String desc;

        TemplateKey(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }

    public String getTouser() {
        return touser;
    }

    public void setTouser(String touser) {
        this.touser = touser;
    }

    public String getTemplate_id() {
        return template_id;
    }

    public void setTemplate_id(String template_id) {
        this.template_id = template_id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, Map<String, String>> getData() {
        return data;
    }

    public void setData(Map<String, Map<String, String>> data) {
        this.data = data;
    }
}
