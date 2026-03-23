package cn.fcr.middleware.sdk.infrastructure.git;


import cn.fcr.middleware.sdk.infrastructure.config.GitConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author 傅崇睿
 * @date 2026/03/20 10:43
 * @description GitCommand
 */
public class GitCommand {

    private final Logger logger = LoggerFactory.getLogger(GitCommand.class);

    private final GitConfig gitConfig;

    public GitCommand(GitConfig gitConfig) {
        this.gitConfig = gitConfig;
    }

    public String diff() throws IOException, InterruptedException {
        // 获取提交对象的全哈希，是绝对引用
        ProcessBuilder logProcessBuilder = new ProcessBuilder("git", "log", "-1", "--pretty=format:%H");
        logProcessBuilder.directory(new File("."));
        Process logProcess = logProcessBuilder.start();

        String latestCommitHash = null;
        try (BufferedReader logReader = new BufferedReader(new InputStreamReader(logProcess.getInputStream(), StandardCharsets.UTF_8))) {
            latestCommitHash = logReader.readLine();
        }
        int logExitCode = logProcess.waitFor();
        if (logExitCode != 0 || latestCommitHash == null) {
            throw new RuntimeException("无法获取 Git Commit Hash，请检查环境。");
        }

        ProcessBuilder diffProcessBuilder = new ProcessBuilder("git", "diff", latestCommitHash + "^", latestCommitHash);
        diffProcessBuilder.directory(new File("."));
        Process diffProcess = diffProcessBuilder.start();

        StringBuilder diffCode = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(diffProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                diffCode.append(line).append("\n");
            }
        }

        int diffExitCode = diffProcess.waitFor();
        if (diffExitCode != 0) {
            throw new RuntimeException("Git 命令执行失败，退出码：" + diffExitCode);
        }

        return diffCode.toString();

    }

    public String commitAndPush(String recommend) {
        try (Git git = Git.cloneRepository()
                .setURI(gitConfig.getGithubReviewLogUri() + ".git")
                .setDirectory(new File("repo"))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitConfig.getGithubToken(), ""))
                .call()) {

            // 1. 创建分支
            String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            File dateFolder = new File("repo/" + dateFolderName);
            if (!dateFolder.exists()) {
                dateFolder.mkdirs();
            }

            // 2. 写文件
            String rawAuthor = gitConfig.getCommitAuthor();
            String cleanAuthor = rawAuthor.split("<")[0].trim();
            String fileName = gitConfig.getCommitProject() + "-" + gitConfig.getCommitBranch() + "-" + cleanAuthor + System.currentTimeMillis() + "-" + RandomStringUtils.randomNumeric(4) + ".md";
            File newFile = new File(dateFolder, fileName);
            try (FileWriter writer = new FileWriter(newFile)) {
                writer.write(recommend);
            }

            // 3. 执行 Git 操作
            git.add().addFilepattern(dateFolderName + "/" + fileName).call();
            git.commit().setMessage("Add new file via GitHub Actions").call();
            git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitConfig.getGithubToken(), "")).call();

            logger.info("openai-code-review git commit and push done! {}", fileName);

            return gitConfig.getGithubReviewLogUri() + "/blob/master/" + dateFolderName + "/" + fileName;
        } catch (Exception e) {
            logger.error("Git commit and push failed", e);
            throw new RuntimeException("Git commit and push failed", e);
        }

    }

    /**
     * 获取提交项目名称
     */
    public String getProject() {
        return gitConfig.getCommitProject();
    }

    /**
     * 获取提交分支名称
     */
    public String getBranch() {
        return gitConfig.getCommitBranch();
    }

    /**
     * 获取提交者信息
     */
    public String getAuthor() {
        return gitConfig.getCommitAuthor();
    }

    /**
     * 获取提交消息
     */
    public String getMessage() {
        return gitConfig.getCommitMessage();
    }

}
