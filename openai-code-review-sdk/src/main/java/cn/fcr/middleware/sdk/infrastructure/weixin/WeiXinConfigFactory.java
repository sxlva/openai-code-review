package cn.fcr.middleware.sdk.infrastructure.weixin;

import cn.fcr.middleware.sdk.infrastructure.config.WeiXinConfig;
import cn.fcr.middleware.sdk.types.utils.EnvUtils;

/**
 * @author 傅崇睿
 * @date 2026/03/21 09:14
 * @description WeiXinConfigFactory
 */
public class WeiXinConfigFactory {

    public static WeiXinConfig create() {
        // 在这里处理繁琐的环境变量逻辑
        String appId = EnvUtils.getEnv("WEIXIN_APPID");
        String secret = EnvUtils.getEnv("WEIXIN_SECRET");
        String toUser = EnvUtils.getEnv("WEIXIN_TOUSER");
        String templateId = EnvUtils.getEnv("WEIXIN_TEMPLATE_ID");

        // 可以在此处校验配置是否完整
        if (appId == null || secret == null) {
            throw new IllegalStateException("微信核心配置缺失，请检查 .env 文件或系统环境变量");
        }

        return new WeiXinConfig(appId, secret, toUser, templateId);
    }

}
