package org.xy.live.im.core.server.common;

import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;


/**
 * WebSocket处理消息的编码过程
 */
public class WebsocketEncoder extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof ImMsg)) {
            super.write(ctx, msg, promise);
            return;
        }
        ImMsg imMsg = (ImMsg) msg;
        ctx.channel().writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(imMsg)));
    }
}