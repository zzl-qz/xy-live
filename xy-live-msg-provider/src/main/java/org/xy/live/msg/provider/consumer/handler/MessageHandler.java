package org.xy.live.msg.provider.consumer.handler;


import org.xy.live.im.dto.ImMsgBody;

/**
 * @Author Daylight
 * @Description
 */
public interface MessageHandler {

    /**
     * 处理im服务投递过来的消息内容
     *
     * @param imMsgBody
     */
    void onMsgReceive(ImMsgBody imMsgBody);


    /**
     * 消息持久化
     */
    boolean msgPersistence(ImMsgBody imMsgBody);

    /**
     * 发送消息持久化消息
     * @param imMsgBody
     */
    void sendMsgPersistence(ImMsgBody imMsgBody);

}
