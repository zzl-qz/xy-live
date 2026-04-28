package org.xy.live.im.core.server.handler.ws;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xy.live.im.core.server.common.ImContextUtils;
import org.xy.live.im.core.server.common.ImMsg;
import org.xy.live.im.core.server.handler.ImHandlerFactory;
import org.xy.live.im.core.server.handler.impl.LogoutMsgHandler;

import java.util.UUID;

/**
 * WebSocket连接处理器
 */
@Component
@ChannelHandler.Sharable
public class WsImServerCoreHandler extends SimpleChannelInboundHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WsImServerCoreHandler.class);

    @Resource
    private ImHandlerFactory imHandlerFactory;
    @Resource
    private LogoutMsgHandler logoutMsgHandler;


    /**
     * 接收消息
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 是ws消息才处理
        if (msg instanceof WebSocketFrame) {
            wsMsgHandler(ctx, (WebSocketFrame) msg);
        }
    }

    /**
     * 客户端正常或意外掉线，都会触发这里
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Long userId = ImContextUtils.getUserId(ctx);
        int appId = ImContextUtils.getAppId(ctx);
        logoutMsgHandler.logoutHandler(ctx, userId, appId);
    }

    private void wsMsgHandler(ChannelHandlerContext ctx, WebSocketFrame msg) {
        // 如果不是文本消息，统一后台不会处理
        if (!(msg instanceof TextWebSocketFrame)) {
            LOGGER.error("[WsImServerCoreHandler] wsMsgHandler, {} msg types not supported", msg.getClass().getName());
            return;
        }
        try {
            // 返回应答消息
            String content = (((TextWebSocketFrame) msg).text());
            LOGGER.info("客户端发送过来的消息是，{}", content);
            JSONObject jsonObject = JSON.parseObject(content, JSONObject.class);

            // {"magic":18673,"code":1003,"bizCode":5555,"len":129,
            // "body":"{\"userId\":387502,\"appId\":10001,\"roomId\":21,\"content\":\"你好\",\
            // "senderName\":\"我\",\"senderAvatar\":\"https://picsum.photos/50/50?random=1\"}"}
            ImMsg imMsg = new ImMsg();
            imMsg.setMagic(jsonObject.getShort("magic"));
            imMsg.setCode(jsonObject.getInteger("code"));
            imMsg.setLen(jsonObject.getInteger("len"));
            imMsg.setBody(jsonObject.getString("body").getBytes());
            // imMsg.setMid(jsonObject.getString("mid"));
            imHandlerFactory.doMsgHandler(ctx, imMsg);
        } catch (Exception e) {
            LOGGER.error("[WsImServerCoreHandler] wsMsgHandler error is ", e);
        }
    }
}