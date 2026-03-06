package cn.fcr.middleware.sdk.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionRequest {

    @Builder.Default
    private String model = "glm-5";

    private List<Prompt> messages;

    @Builder.Default
    private Double temperature = 0.2;
    @Builder.Default
    private Boolean stream = false;
    @Builder.Default
    private Thinking thinking = new Thinking("enabled"); // 开启思维链

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prompt {
        private String role;
        private String content;
    }

    @Data
    @AllArgsConstructor
    public static class Thinking {
        private String type;
    }

}
