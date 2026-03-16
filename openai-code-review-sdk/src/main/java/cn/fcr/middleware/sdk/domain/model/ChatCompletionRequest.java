package cn.fcr.middleware.sdk.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 对话补全同步请求模型。
 * <p>
 * 封装模型参数、消息上下文与采样配置，用于构建提交给大模型服务端的请求体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionRequest {

    /** 目标模型标识。 */
    @Builder.Default
    private String model = "glm-5";

    /** 对话消息序列，按时间顺序传入上下文。 */
    private List<Prompt> messages;

    /** 采样温度，值越低输出越稳定。 */
    @Builder.Default
    private Double temperature = 0.2;
    /** 是否启用流式输出。 */
    @Builder.Default
    private Boolean stream = false;
    /** 思维链开关配置。 */
    @Builder.Default
    private Thinking thinking = new Thinking("enabled"); // 开启思维链

    /** 单条对话消息结构。 */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prompt {
        /** 角色，例如 system/user/assistant。 */
        private String role;
        /** 角色对应的消息内容。 */
        private String content;
    }

    /** 推理模式配置。 */
    @Data
    @AllArgsConstructor
    public static class Thinking {
        /** 推理类型，如 enabled/disabled。 */
        private String type;
    }

}
