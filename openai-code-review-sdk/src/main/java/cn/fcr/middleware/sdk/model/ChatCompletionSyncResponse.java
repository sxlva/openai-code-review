package cn.fcr.middleware.sdk.model;

import lombok.Data;
import java.util.List;

@Data
public class ChatCompletionSyncResponse {

    private Long created;
    private List<Choice> choices;
    private Usage usage; // 用量统计

    @Data
    public static class Choice {
        private Integer index;
        private Message message;
        private String finish_reason; //停止原因
    }

    @Data
    public static class Message {
        private String role;
        private String content;
        private String reasoning_content;
    }

    @Data
    public static class Usage {
        private Integer completion_tokens;
        private Integer prompt_tokens;
        private PromptTokensDetails prompt_tokens_details;
        private Integer total_tokens;
    }

    @Data
    public static class PromptTokensDetails {
        private Integer cached_tokens;
    }

}
