package org.xy.live.im.core.server.service;

import org.xy.live.im.dto.ImMsgBody;

public interface IRouterHandlerService {


    /**
     * 当收到来自Router定向转发的业务服务的请求时，进行处理
     */
    void onReceive(ImMsgBody imMsgBody);

    /**
     * 发送消息给客户端
     *
     * @param imMsgBody
     * @return
     */
    boolean sendMsgToClient(ImMsgBody imMsgBody);

}