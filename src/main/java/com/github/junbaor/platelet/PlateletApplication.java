package com.github.junbaor.platelet;

import com.github.junbaor.platelet.listener.WebhookListener;
import com.github.junbaor.platelet.msg.GroupMsg;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.webhook.WebHookManager;
import org.slf4j.MDC;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@SpringBootApplication
public class PlateletApplication implements InitializingBean {

    public static void main(String[] args) {
        SpringApplication.run(PlateletApplication.class, args);
    }

    private WebHookManager webHookManager = new WebHookManager();

    @Inject
    private WebhookListener listener;

    @Override
    public void afterPropertiesSet() throws Exception {
        webHookManager.addListener(listener);
    }

    @RequestMapping
    public String index() {
        return "hello";
    }

    @RequestMapping(value = "webhook/{key}")
    public String webhook(@PathVariable("key") String key, @RequestParam(required = false, name = "noticeMembers") List<String> noticeMemberMobileList, HttpServletRequest request) {
        if (StringUtils.isEmpty(key)) {
            return "error";
        }

        // 把微信机器人的 key 放到上下文
        MDC.put("key", key);

        try {
            webHookManager.handleEvent(request);
            if ("1".equals(MDC.get("isNotice")) && !CollectionUtils.isEmpty(noticeMemberMobileList)) {
                GroupMsg.getInstance(key).sendTextMsg("⬆️请处理 ", null, noticeMemberMobileList);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "error";
        }
        return "ok";
    }

}
