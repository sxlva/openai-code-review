package cn.fcr.middleware.sdk.infrastructure.openai;

import cn.fcr.middleware.sdk.infrastructure.openai.DTO.ChatCompletionRequestDTO;
import cn.fcr.middleware.sdk.infrastructure.openai.DTO.ChatCompletionSyncResponseDTO;

/**
 * @author 傅崇睿
 * @date 2026/03/20 13:52
 * @description IOpenAI
 */
public interface IOpenAI {

    ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception;

}
