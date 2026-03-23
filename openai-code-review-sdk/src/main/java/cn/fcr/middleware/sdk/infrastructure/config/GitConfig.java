package cn.fcr.middleware.sdk.infrastructure.config;

/**
 * @author 傅崇睿
 * @date 2026/03/21 10:20
 * @description GitConfig
 */
public final class GitConfig {

    // Github 配置
    private final String githubReviewLogUri;
    private final String githubToken;

    // 工程配置 - 自动获取
    private final String commitProject;
    private final String commitBranch;
    private final String commitAuthor;
    private final String commitMessage;

    public GitConfig(String githubReviewLogUri, String githubToken, String commitProject, String commitBranch, String commitAuthor, String commitMessage) {
        this.githubReviewLogUri = githubReviewLogUri;
        this.githubToken = githubToken;
        this.commitProject = commitProject;
        this.commitBranch = commitBranch;
        this.commitAuthor = commitAuthor;
        this.commitMessage = commitMessage;
    }

    public String getGithubReviewLogUri() {
        return githubReviewLogUri;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public String getCommitProject() {
        return commitProject;
    }

    public String getCommitBranch() {
        return commitBranch;
    }

    public String getCommitAuthor() {
        return commitAuthor;
    }

    public String getCommitMessage() {
        return commitMessage;
    }
}
