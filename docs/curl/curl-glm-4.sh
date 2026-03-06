#!/bin/bash

curl -X POST "https://open.bigmodel.cn/api/paas/v4/chat/completions" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer 23617b1f32e94db9b6d8e4553dc1a2a0.JagxNFNFTYb9XKf6" \
    -d '{
        "model": "glm-4.6",
        "messages": [
            {
                "role": "user",
                "content": "你是一个高级编程架构师，请对以下 Java 代码片段进行评审：\n\nProcessBuilder processBuilder = new ProcessBuilder(\"git\", \"diff\", \"HEAD~1\", \"HEAD\").directory(new File(\".\"));\nProcess process = processBuilder.start();\nBufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));\nStringBuilder diffCode = new StringBuilder();\nString line;\nwhile ((line = reader.readLine()) != null) { diffCode.append(line).append(\"\\n\"); }\nint exitCode = process.waitFor();"
            }
        ],
        "thinking": {
            "type": "enabled"
        },
        "max_tokens": 2048,
        "temperature": 1.0
    }'