package org.xy.live.im.core.server.handler.impl;

import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.xy.live.framework.redis.starter.key.ImCoreServerProviderCacheKeyBuilder;
import org.xy.live.im.constants.ImMsgCodeEnum;
import org.xy.live.im.core.server.common.ChannelHandlerContextCache;
import org.xy.live.im.core.server.common.ImContextUtils;
import org.xy.live.im.core.server.common.ImMsg;
import org.xy.live.im.core.server.handler.SimpleHandler;
import org.xy.live.im.core.server.interfaces.constants.ImCoreServerConstants;
import org.xy.live.im.core.server.service.IMsgAckCheckService;
import org.xy.live.im.dto.ImMsgBody;

/**
 * 确认消息处理器
 */
@Component
public class AckImMsgHandler implements SimpleHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AckImMsgHandler.class);
    private ImCoreServerProviderCacheKeyBuilder cacheKeyBuilder;

    @Resource
    private IMsgAckCheckService iMsgAckCheckService;

    @Override
    public void handler(ChannelHandlerContext ctx, ImMsg imMsg) {
        // 执行到这里说明已经接收到ack类型的信号，那么就是去删除一下之前记录的ack信号
        Integer appId = ImContextUtils.getAppId(ctx);
        Long userId = ImContextUtils.getUserId(ctx);
        if (appId == null && userId == null) {
            ctx.close();
            LOGGER.error("appId or userId is null");
            throw new RuntimeException("attr error");
        }

        // 删除记录
        iMsgAckCheckService.doMsgAck(JSON.parseObject(imMsg.getBody(), ImMsgBody.class));
        LOGGER.info("删除ACK信号成功");
    }

}