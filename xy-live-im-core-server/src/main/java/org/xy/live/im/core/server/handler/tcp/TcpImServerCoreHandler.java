package org.xy.live.im.core.server.handler.tcp;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.xy.live.im.core.server.common.ImContextUtils;
import org.xy.live.im.core.server.common.ImMsg;
import org.xy.live.im.core.server.handler.ImHandlerFactory;
import org.xy.live.im.core.server.handler.impl.LogoutMsgHandler;

/**
 * 接收外界所有消息的处理器(处理TCP连接的处理器)
 */
@Component
@ChannelHandler.Sharable // 允许被多个 Channel的 Pipeline共享
public class TcpImServerCoreHandler extends SimpleChannelInboundHandler {

    @Resource
    private ImHandlerFactory imHandlerFactory;


    @Resource
    private LogoutMsgHandler logoutMsgHandler;

    /**
     * 在 Netty 框架中处理入站消息时被调用
     *
     * @param channelHandlerContext
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
        if(!(msg instanceof ImMsg)) {
            throw new IllegalArgumentException("error msg, msg is :" + msg);
        }
        ImMsg imMsg = (ImMsg) msg;
        imHandlerFactory.doMsgHandler(channelHandlerContext, imMsg);
    }

    /**
     * 只要是断线，不管是正常还是意外都会回调到这里(那我们的做法就是直接将这个用户Id移除掉)
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Long userId = ImContextUtils.getUserId(ctx);
        Integer appId = ImContextUtils.getAppId(ctx);
        if (userId != null && appId != null) {
            logoutMsgHandler.handlerLogout(userId, appId);
        }
    }
}