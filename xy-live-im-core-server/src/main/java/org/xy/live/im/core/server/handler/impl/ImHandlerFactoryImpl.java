package org.xy.live.im.core.server.handler.impl;

import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.xy.live.im.constants.ImMsgCodeEnum;
import org.xy.live.im.core.server.common.ImMsg;
import org.xy.live.im.core.server.handler.ImHandlerFactory;
import org.xy.live.im.core.server.handler.SimpleHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 工厂模式+策略模式
 */
@Component
public class ImHandlerFactoryImpl implements ImHandlerFactory, InitializingBean {
    
    private static Map<Integer, SimpleHandler> simpleHandlerMap = new HashMap<>();

    @Resource
    private ApplicationContext applicationContext;

    @Override
    public void doMsgHandler(ChannelHandlerContext ctx, ImMsg imMsg) {
        SimpleHandler simpleHandler = simpleHandlerMap.get(imMsg.getCode());
        if(simpleHandler == null) {
            throw new IllegalArgumentException("msg code is error, code is :" + imMsg.getCode());
        }
        // 策略模式
        simpleHandler.handler(ctx, imMsg);
    }

    /**
     * Bean初始化的时候执行（工厂模式）
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        //登录消息包：登录token验证，channel 和 userId 关联
        simpleHandlerMap.put(ImMsgCodeEnum.IM_LOGIN_MSG.getCode(), applicationContext.getBean(LoginMsgHandler.class));
        //登出消息包：正常断开im连接时发送的
        simpleHandlerMap.put(ImMsgCodeEnum.IM_LOGOUT_MSG.getCode(), applicationContext.getBean(LogoutMsgHandler.class));
        //业务消息包：最常用的消息类型，例如我们的im收发数据
        simpleHandlerMap.put(ImMsgCodeEnum.IM_BIZ_MSG.getCode(), applicationContext.getBean(BizImMsgHandler.class));
        //心跳消息包：定时给im发送心跳包
        simpleHandlerMap.put(ImMsgCodeEnum.IM_HEARTBEAT_MSG.getCode(), applicationContext.getBean(HeartBeatImMsgHandler.class));
        // ACK消息确认包
        simpleHandlerMap.put(ImMsgCodeEnum.IM_ACK_MSG.getCode(), applicationContext.getBean(AckImMsgHandler.class));
    }
}