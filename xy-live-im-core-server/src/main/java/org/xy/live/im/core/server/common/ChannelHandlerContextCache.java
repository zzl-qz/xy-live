package org.xy.live.im.core.server.common;

import java.util.HashMap;
import java.util.Map;

import io.netty.channel.ChannelHandlerContext;

/**
 * 封装ChannelHandlerContext的缓存，将已建立连接的ChannelHandlerContext放到这里
 * 其实就是一个map集合，然后将已经建立连接的就塞到里面
 *
 * ChannelHandlerContext 就是上下文信息，当用户登录连接之后就会生成一个这个，然后我们需要存储起来
 *
 */
public class ChannelHandlerContextCache {

    /**
     * 当前IM服务启动的时候，在Nacos对外暴露的ip和端口
     */
    private static String SERVER_IP_ADDRESS = "";

    
    private static Map<Long, ChannelHandlerContext> channelHandlerContextMap = new HashMap<>();

    // 取
    public static ChannelHandlerContext get(Long userId) {
        return channelHandlerContextMap.get(userId);
    }

    // 增
    public static void put(Long userId, ChannelHandlerContext channelHandlerContext) {
        channelHandlerContextMap.put(userId, channelHandlerContext);
    }

    // 删
    public static void remove(Long userId) {
        channelHandlerContextMap.remove(userId);
    }


    public static void setServerIpAddress(String serverIpAddress) {
        ChannelHandlerContextCache.SERVER_IP_ADDRESS = serverIpAddress;
    }

    public static String getServerIpAddress() {
        return SERVER_IP_ADDRESS;
    }

}