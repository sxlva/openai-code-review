package cn.fcr.middleware.sdk.domain.service;

import cn.fcr.middleware.sdk.infrastructure.git.GitCommand;
import cn.fcr.middleware.sdk.infrastructure.openai.IOpenAI;
import cn.fcr.middleware.sdk.infrastructure.weixin.WeiXin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author 傅崇睿
 * @date 2026/03/21 09:45
 * @description AbstractOpenAiCodeReviewService
 */
public abstract class AbstractOpenAiCodeReviewService implements IOpenAiCodeReviewService {

    private final Logger logger = LoggerFactory.getLogger(AbstractOpenAiCodeReviewService.class);

    protected final GitCommand gitCommand;
    protected final IOpenAI openAI;
    protected final WeiXin weiXin;

    public AbstractOpenAiCodeReviewService(GitCommand gitCommand, IOpenAI openAI, WeiXin weiXin) {
        this.gitCommand = gitCommand;
        this.openAI = openAI;
        this.weiXin = weiXin;
    }

    @Override
    public void exec() {
        try {
            // 1. 获取提交日志
            String diffCode = getDiffCode();
            // 2. 调用 OpenAI 进行代码评审
            String recommend = codeReview(diffCode);
            // 3. 记录评审结果
            String logUrl = recordCodeReview(recommend);
            // 4. 将评审结果发送到微信
            pushMessage(logUrl);
        } catch (Exception e) {
            logger.error("openai-code-review error", e);
        }
    }

    protected abstract String getDiffCode() throws IOException, InterruptedException;

    protected abstract String codeReview(String diffCode) throws Exception;

    protected abstract String recordCodeReview(String recommend) throws Exception;

    protected abstract void pushMessage(String logUrl) throws Exception;
}
