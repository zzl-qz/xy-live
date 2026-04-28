package org.xy.live.im.router.provider.service;

import org.xy.live.im.dto.ImMsgBody;

import java.util.List;

public interface ImRouterService {

    /**
     * 从redis中拿到用户绑定的IP地址，注入到rpc上下文中
     *
     * @param imMsgBody
     * @return
     */
    boolean sendMsg(ImMsgBody imMsgBody);

    /**
     * 实现在直播间内进行消息的批量推送
     *
     * @param imMsgBodyList
     */
    void batchSendMsg(List<ImMsgBody> imMsgBodyList);
}