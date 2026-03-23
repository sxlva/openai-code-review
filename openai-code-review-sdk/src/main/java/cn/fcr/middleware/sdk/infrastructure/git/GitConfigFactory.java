package cn.fcr.middleware.sdk.infrastructure.git;

import cn.fcr.middleware.sdk.infrastructure.config.GitConfig;

/**
 * @author 傅崇睿
 * @date 2026/03/23 10:41
 * @description GitConfigFactory
 */
public class GitConfigFactory {

    public static GitConfig create() {
        String githubReviewLogUri = System.getenv("GITHUB_REVIEW_LOG_URI");
        String githubToken = System.getenv("GITHUB_TOKEN");
        String commitProject = System.getenv("COMMIT_PROJECT");
        String commitBranch = System.getenv("COMMIT_BRANCH");
        String commitAuthor = System.getenv("COMMIT_AUTHOR");
        String commitMessage = System.getenv("COMMIT_MESSAGE");

        // 可以在此处校验配置是否完整
        if (githubReviewLogUri == null || githubToken == null || commitProject == null || commitBranch == null || commitAuthor == null || commitMessage == null) {
            throw new IllegalStateException("Git核心配置缺失，请检查 .env 文件或系统环境变量");
        }

        return new GitConfig(githubReviewLogUri, githubToken, commitProject, commitBranch, commitAuthor, commitMessage);

    }

}
