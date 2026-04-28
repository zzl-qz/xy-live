package org.xy.live.im.core.server.service.impl;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.xy.live.im.constants.ImMsgCodeEnum;
import org.xy.live.im.core.server.common.ChannelHandlerContextCache;
import org.xy.live.im.core.server.common.ImMsg;
import org.xy.live.im.core.server.service.IMsgAckCheckService;
import org.xy.live.im.core.server.service.IRouterHandlerService;
import org.xy.live.im.dto.ImMsgBody;

import java.util.UUID;

@Service
public class RouterHandlerServiceImpl implements IRouterHandlerService {

    @Resource
    private IMsgAckCheckService iMsgAckCheckService;


    @Override
    public void onReceive(ImMsgBody imMsgBody) {
        // 消息到达时，对应客户端未下线才进行反向推送操作
        if (sendMsgToClient(imMsgBody)) {
            // 当im服务器推送了消息给到客户端，然后我们需要记录下ack
            iMsgAckCheckService.recordMsgAck(imMsgBody, 1);
            // 发送一条延时消息
            iMsgAckCheckService.sendDelayMsg(imMsgBody);
        }
    }


    /**
     * 真正发送的逻辑
     * @param imMsgBody
     * @return
     */
    @Override
    public boolean sendMsgToClient(ImMsgBody imMsgBody) {
        Long userId = imMsgBody.getUserId();
        ChannelHandlerContext ctx = ChannelHandlerContextCache.get(userId);
        if (ctx != null) {
            // 进行消息的推送
            ImMsg imMsg = ImMsg.build(ImMsgCodeEnum.IM_BIZ_MSG.getCode(), JSON.toJSONString(imMsgBody));
            ctx.writeAndFlush(imMsg);
            return true;
        }
        return false;
    }

}
