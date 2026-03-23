package cn.fcr.middleware.sdk.infrastructure.config;

/**
 * @author 傅崇睿
 * @date 2026/03/20 15:42
 * @description 微信配置上下文
 */
public final class WeiXinConfig {

    private final String appId;
    private final String secret;
    private final String toUser;
    private final String templateId;

    public WeiXinConfig(String appId, String secret, String TOUSER, String TEMPLATE_ID) {
        this.appId = appId;
        this.secret = secret;
        this.toUser = TOUSER;
        this.templateId = TEMPLATE_ID;
    }

    public String getAppId() {
        return appId;
    }

    public String getSecret() {
        return secret;
    }

    public String getToUser() {
        return toUser;
    }

    public String getTemplateId() {
        return templateId;
    }
}
