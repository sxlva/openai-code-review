package cn.fcr.middleware.sdk;

import cn.fcr.middleware.sdk.domain.service.impl.OpenAiCodeReviewService;
import cn.fcr.middleware.sdk.infrastructure.config.AIConfig;
import cn.fcr.middleware.sdk.infrastructure.config.GitConfig;
import cn.fcr.middleware.sdk.infrastructure.config.WeiXinConfig;
import cn.fcr.middleware.sdk.infrastructure.git.GitCommand;
import cn.fcr.middleware.sdk.infrastructure.git.GitConfigFactory;
import cn.fcr.middleware.sdk.infrastructure.openai.AIConfigFactory;
import cn.fcr.middleware.sdk.infrastructure.openai.IOpenAI;
import cn.fcr.middleware.sdk.infrastructure.openai.impl.ChatGLM;
import cn.fcr.middleware.sdk.infrastructure.weixin.WeiXin;
import cn.fcr.middleware.sdk.infrastructure.weixin.WeiXinConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class OpenAiCodeReview {

    public static final Logger logger = LoggerFactory.getLogger(OpenAiCodeReview.class);

    public static void main(String[] args) {

        GitConfig gitConfig = GitConfigFactory.create();
        WeiXinConfig weiXinConfig = WeiXinConfigFactory.create();
        AIConfig aiConfig = AIConfigFactory.create();

        GitCommand gitCommand = new GitCommand(gitConfig);
        WeiXin weiXin = new WeiXin(weiXinConfig);
        IOpenAI openAI = new ChatGLM(aiConfig);

        OpenAiCodeReviewService openAiCodeReviewService = new OpenAiCodeReviewService(gitCommand, openAI, weiXin);
        openAiCodeReviewService.exec();

        logger.info("openai-code-review done!");

    }

}
