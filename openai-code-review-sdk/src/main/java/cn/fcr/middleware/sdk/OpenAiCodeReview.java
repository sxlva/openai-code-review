package cn.fcr.middleware.sdk;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class OpenAiCodeReview {

    public static void main(String[] args) throws Exception{
        System.out.println("测试执行");

        // 1. 代码检出
        ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "HEAD~1", "HEAD")
                .directory(new File("."));

        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        StringBuilder diffCode = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            diffCode.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("Exited with code:" + exitCode);
            System.out.println("评审代码内容：\n" + diffCode);
        } else {
            System.err.println("Git 命令执行失败，退出码：" + exitCode);
        }

    }

}
