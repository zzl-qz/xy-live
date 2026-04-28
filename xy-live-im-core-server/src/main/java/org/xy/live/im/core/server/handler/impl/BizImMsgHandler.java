package org.xy.live.im.core.server.handler.impl;

import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xy.live.im.core.server.common.ImContextUtils;
import org.xy.live.im.core.server.common.ImMsg;
import org.xy.live.im.core.server.handler.SimpleHandler;

import static org.xy.live.common.interfaces.topic.ImCoreServerProviderTopicNames.XY_LIVE_IM_BIZ_MSG_TOPIC;


/**
 * 业务消息处理器
 */
@Component
public class BizImMsgHandler implements SimpleHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BizImMsgHandler.class);

    @Resource
    private MQProducer mqProducer;

    @Override
    public void handler(ChannelHandlerContext ctx, ImMsg imMsg) {
        // 1. 信息基本校验
        Long userId = ImContextUtils.getUserId(ctx);
        Integer appId = ImContextUtils.getAppId(ctx);
        Integer roomId = ImContextUtils.getRoomId(ctx);
        if (appId == null || userId == null || roomId == null) {
            ctx.close();
            LOGGER.error(" ctx error, imMsg {}", imMsg);
            throw new RuntimeException("token error");
        }
        byte[] body = imMsg.getBody();
        if (body == null || body.length == 0) {
            ctx.close();
            LOGGER.error(" ctx error, imMsg {}", imMsg);
            throw new RuntimeException("token error");
        }


        // 3. 发送到消息队列中
        Message message = new Message();
        message.setTopic(XY_LIVE_IM_BIZ_MSG_TOPIC);
        message.setBody(body);
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info(" mqSend success, sendResult {}", sendResult);
        } catch (Exception e) {
            LOGGER.error(" mqSend error, errMsg {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}