package cn.fcr.middleware.sdk.domain.service.impl;

import cn.fcr.middleware.sdk.domain.service.AbstractOpenAiCodeReviewService;
import cn.fcr.middleware.sdk.infrastructure.git.GitCommand;
import cn.fcr.middleware.sdk.infrastructure.openai.DTO.ChatCompletionRequestDTO;
import cn.fcr.middleware.sdk.infrastructure.openai.DTO.ChatCompletionSyncResponseDTO;
import cn.fcr.middleware.sdk.infrastructure.openai.IOpenAI;
import cn.fcr.middleware.sdk.infrastructure.weixin.DTO.TemplateMessageDTO;
import cn.fcr.middleware.sdk.infrastructure.weixin.WeiXin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 傅崇睿
 * @date 2026/03/21 09:54
 * @description OpenAiCodeReviewService
 */
public class OpenAiCodeReviewService extends AbstractOpenAiCodeReviewService {

    public OpenAiCodeReviewService(GitCommand gitCommand, IOpenAI openAI, WeiXin weiXin) {
        super(gitCommand, openAI, weiXin);
    }

    @Override
    protected String getDiffCode() throws IOException, InterruptedException {
        return gitCommand.diff();
    }

    @Override
    protected String codeReview(String diffCode) throws Exception {
        // 构建请求体
        List<ChatCompletionRequestDTO.Prompt> prompts = new ArrayList<>();
        prompts.add(new ChatCompletionRequestDTO.Prompt("system", "你是一个高级编程架构师，精通各类场景方案、架构设计和编程语言请，请您根据git diff记录，对代码做出评审。"));
        prompts.add(new ChatCompletionRequestDTO.Prompt("user", "代码如下:\n" + diffCode));

        ChatCompletionRequestDTO chatCompletionRequest = ChatCompletionRequestDTO.builder()
                .messages(prompts)
                .build();

        ChatCompletionSyncResponseDTO completions = openAI.completions(chatCompletionRequest);

        // 提取评审内容
        ChatCompletionSyncResponseDTO.Message message = completions.getChoices().get(0).getMessage();
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

    @Override
    protected String recordCodeReview(String recommend) throws Exception {
        return gitCommand.commitAndPush(recommend);
    }

    @Override
    protected void pushMessage(String logUrl) throws Exception {
        Map<String, Map<String, String>> data = new HashMap<>();
        String rawAuthor = gitCommand.getAuthor();
        String cleanAuthor = rawAuthor.split("<")[0].trim();
        TemplateMessageDTO.put(data, TemplateMessageDTO.TemplateKey.REPO_NAME, gitCommand.getProject());
        TemplateMessageDTO.put(data, TemplateMessageDTO.TemplateKey.BRANCH_NAME, gitCommand.getBranch());
        TemplateMessageDTO.put(data, TemplateMessageDTO.TemplateKey.COMMIT_AUTHOR, cleanAuthor);
        TemplateMessageDTO.put(data, TemplateMessageDTO.TemplateKey.COMMIT_MESSAGE, gitCommand.getMessage());
        weiXin.sendTemplateMessage(logUrl, data);
    }
}
