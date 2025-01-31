package com.github.junbaor.platelet.msg;

import com.alibaba.fastjson.JSONObject;
import com.github.junbaor.platelet.util.AppUtils;
import lombok.extern.slf4j.Slf4j;
import net.dongliu.requests.Requests;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Slf4j
public class GroupMsg implements IGroupMsg {

    private static String WORK_WECHAT_API_URL_PREFIX = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=";

    private String botKey;

    private GroupMsg() {
    }

    public static GroupMsg getInstance(String botKey) {
        GroupMsg groupMsg = new GroupMsg();
        groupMsg.botKey = botKey;
        return groupMsg;
    }

    @Override
    public boolean sendTextMsg(String content, List<String> mentionedList, List<String> mentionedMobileList) {
        LinkedHashMap<Object, Object> hashMap = AppUtils.map(
                "msgtype", "text",
                "text", AppUtils.map(
                        "content", content,
                        "mentioned_list", mentionedList,
                        "mentioned_mobile_list", mentionedMobileList
                )
        );
        return sendHttpRequest(hashMap);
    }

    @Override
    public boolean sendMarkdownMsg(String markdown) {
        LinkedHashMap<Object, Object> hashMap = AppUtils.map(
                "msgtype", "markdown",
                "markdown", AppUtils.map(
                        "content", markdown
                )
        );

        return sendHttpRequest(hashMap);
    }

    private boolean sendHttpRequest(LinkedHashMap<Object, Object> hashMap) {
        boolean flag = false;

        log.info("wechat request:{}", JSONObject.toJSONString(hashMap));
        String response = Requests.post(getWorkWechatUrl())
                .jsonBody(hashMap)
                .timeout(2000, 2000)
                .send().readToText();
        log.info("wechat reponse:{}", response);

        if (!StringUtils.isEmpty(response)) {
            JSONObject jsonObject = JSONObject.parseObject(response);
            Integer errcode = jsonObject.getInteger("errcode");
            flag = Objects.equals(errcode, 0);
        }

        return flag;
    }

    private String getWorkWechatUrl() {
        return WORK_WECHAT_API_URL_PREFIX + botKey;
    }
}
