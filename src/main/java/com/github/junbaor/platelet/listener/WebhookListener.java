package com.github.junbaor.platelet.listener;

import com.github.junbaor.platelet.msg.GroupMsg;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.webhook.*;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import javax.inject.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Named
@Slf4j
public class WebhookListener implements WebHookListener {

    @Override
    public void onPushEvent(PushEvent event) {
        String botKey = MDC.get("key");

        List<EventCommit> commits = event.getCommits();
        Collections.reverse(commits);

        if (!checkTargetBranch(event.getBranch())) {
            return;
        }

        StringBuilder content = new StringBuilder();
        content.append("**")
                .append(event.getUserName())
                .append("** 向仓库 <font color=\"info\">")
                .append(event.getProject().getNamespace())
                .append("</font>[").append(event.getProject().getName()).append("]")
                .append("(").append(event.getProject().getWebUrl()).append(")")
                .append(" 推送代码到分支 ")
                .append("[").append(event.getBranch()).append("]")
                .append("(").append(event.getProject().getWebUrl()).append("/tree/").append(event.getBranch()).append(")")
                .append("\n提交记录:\n");

        for (EventCommit commit : commits) {
            String shortId = commit.getId().substring(0, 6);
            content.append("> ")
                    .append("[").append(shortId).append("]")
                    .append("(").append(commit.getUrl()).append(")")
                    .append(" : ")
                    .append(commit.getMessage())
                    .append("\n");
        }

        GroupMsg.getInstance(botKey).sendMarkdownMsg(content.toString());
    }

    @Override
    public void onMergeRequestEvent(MergeRequestEvent event) {
        String botKey = MDC.get("key");

        log.debug(event.toString());

        StringBuilder content = new StringBuilder();
        String sourceBranch = event.getObjectAttributes().getSourceBranch();
        String sourceBranchUrl = event.getProject().getWebUrl() + "/tree/" + sourceBranch;
        String targetBranch = event.getObjectAttributes().getTargetBranch();
        String targetBranchUrl = event.getProject().getWebUrl() + "/tree/" + targetBranch;
        String state = event.getObjectAttributes().getState();

        if (!checkTargetBranch(targetBranch)) {
            return;
        }

        String action = "创建";
        boolean isNotice = true;
        if (event.getChanges().getTitle() != null) {
            action = "更新";
        }
        if (Objects.equals(state, "merged")) {
            action = "合并";
            isNotice = false;
        }
        if (Objects.equals(state, "closed")) {
            action = "关闭";
            isNotice = false;
        }

        content.append("**")
                .append(event.getUser().getName())
                .append("** 向仓库 <font color=\"info\">")
                .append(event.getProject().getNamespace())
                .append("</font>[").append(event.getProject().getName()).append("]")
                .append("(").append(event.getProject().getWebUrl()).append(")")
                .append(" 对合并请求进行了**")
                .append(action)
                .append("**操作\n> 分支：从 ")
                .append("[").append(sourceBranch).append("]")
                .append("(").append(sourceBranchUrl).append(")")
                .append(" 到 ")
                .append("[").append(targetBranch).append("]")
                .append("(").append(targetBranchUrl).append(")")
                .append("\n")
                .append("> ")
                .append("标题：").append(event.getObjectAttributes().getTitle())
                .append("\n> 状态 : ").append(state).append(" | ").append(event.getObjectAttributes().getMergeStatus())
                .append("\n> [链接](").append(event.getObjectAttributes().getUrl()).append(")：")
                .append(event.getObjectAttributes().getUrl()
                );

        GroupMsg.getInstance(botKey).sendMarkdownMsg(content.toString());
        if(isNotice) {
            // 通知处理人
            String noticeMemberMobileStr = MDC.get("noticeMemberMobileList");
            if (!StringUtils.isEmpty(noticeMemberMobileStr)) {
                List<String> noticeMemberMobileList = Arrays.asList(noticeMemberMobileStr.split(","));
                GroupMsg.getInstance(botKey).sendTextMsg("⬆️请处理 ", null, noticeMemberMobileList);
            }
        }
    }

    @Override
    public void onTagPushEvent(TagPushEvent event) {
        String botKey = MDC.get("key");

        log.debug(event.toString());

        StringBuilder content = new StringBuilder();
        String projectName = event.getProject().getName();
        String projectUrl = event.getProject().getWebUrl();
        String tagName = event.getRef().replaceAll("refs/tags/", "");
        String tagUrl = projectUrl + "/tree/" + tagName;

        content.append(event.getUserName())
                .append(" pushed tag ")
                .append("[").append(tagName).append("]")
                .append("(").append(tagUrl).append(")")
                .append(" at repository ")
                .append("[").append(projectName).append("]")
                .append("(").append(projectUrl).append(")");

        GroupMsg.getInstance(botKey).sendMarkdownMsg(content.toString());
    }

    private boolean checkTargetBranch(String branchName) {
        String branchStr = MDC.get("branchList");
        if (StringUtils.isEmpty(branchStr)) {
            return true;
        }

        List<String> branchList = Arrays.asList(branchStr.split(","));
        if (branchList.contains(branchName)) {
            return true;
        }
        return false;
    }
}
