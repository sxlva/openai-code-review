package cn.fcr.middleware.sdk.domain.model;

import lombok.Data;
import java.util.List;

/**
 * 对话补全同步响应模型。
 * <p>
 * 对应服务端一次完整推理返回，包含候选结果与 token 用量统计。
 */
@Data
public class ChatCompletionSyncResponse {

    /** 响应创建时间戳。 */
    private Long created;
    /** 候选输出列表，通常使用首个 choice。 */
    private List<Choice> choices;
    private Usage usage; // 用量统计

    /** 候选输出单元。 */
    @Data
    public static class Choice {
        /** 候选序号。 */
        private Integer index;
        /** 模型输出消息体。 */
        private Message message;
        private String finish_reason; //停止原因
    }

    /** 模型输出消息内容。 */
    @Data
    public static class Message {
        /** 输出角色。 */
        private String role;
        /** 最终可读输出文本。 */
        private String content;
        /** 推理过程文本（若服务端开启返回）。 */
        private String reasoning_content;
    }

    /** token 用量统计。 */
    @Data
    public static class Usage {
        /** 生成阶段消耗 token。 */
        private Integer completion_tokens;
        /** 输入提示消耗 token。 */
        private Integer prompt_tokens;
        /** 输入 token 细分信息。 */
        private PromptTokensDetails prompt_tokens_details;
        /** 总 token 消耗。 */
        private Integer total_tokens;
    }

    /** 输入 token 明细。 */
    @Data
    public static class PromptTokensDetails {
        /** 命中缓存的 token 数量。 */
        private Integer cached_tokens;
    }

}
