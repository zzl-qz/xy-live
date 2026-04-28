package org.xy.live.im.router.provider.service.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.MapUtil;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xy.live.im.constants.ImConstants;
import org.xy.live.im.core.server.interfaces.constants.ImCoreServerConstants;
import org.xy.live.im.core.server.interfaces.rpc.IRouterHandlerRpc;
import org.xy.live.im.dto.ImMsgBody;
import org.xy.live.im.router.provider.service.ImRouterService;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ImRouterServiceImpl implements ImRouterService {

    @DubboReference
    private IRouterHandlerRpc routerHandlerRpc;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final Logger LOGGER = LoggerFactory.getLogger(ImRouterServiceImpl.class);


    /**
     * 从redis中拿到用户绑定的IP地址，注入到rpc上下文中
     *
     * @param imMsgBody
     * @return
     */
    @Override
    public boolean sendMsg(ImMsgBody imMsgBody) {
        // 从redis中拿到当前用户是绑定的哪一台core-server
        System.out.println("[ImRouterServiceImpl1]");
        String bindAddress = stringRedisTemplate.opsForValue().get(ImCoreServerConstants.IM_BIND_IP_KEY + imMsgBody.getAppId() + ":" + imMsgBody.getUserId());
        if (StringUtils.isEmpty(bindAddress)) {
            return false;
        }
        bindAddress = bindAddress.substring(0, bindAddress.indexOf("%"));//新加的：去除后面拼接的userId
        RpcContext.getContext().set("ip", bindAddress);
        routerHandlerRpc.sendMsg(imMsgBody);
        System.out.println("[ImRouterServiceImpl2]");
        return true;
    }

    /**
     * 不能每个ImMsgBody都调用一次RPC，因为这样将会有很大的网络消耗，
     * 所以我们要根据IP对ImMSgBody进行分组，然后再多次批量转发
     */
    @Override
    public void batchSendMsg(List<ImMsgBody> imMsgBodyList) {
        LOGGER.info("####################");
        //我们需要对IP进行分组，对相同IP服务器的userIdList进行分组，每组进行一此调用，减少网络开销
        String cacheKeyPrefix = ImCoreServerConstants.IM_BIND_IP_KEY + imMsgBodyList.get(0).getAppId() + ":";
        List<String> cacheKeyList = imMsgBodyList.stream().map(ImMsgBody::getUserId).map(userId -> cacheKeyPrefix + userId).collect(Collectors.toList());
        //批量取出每个用户绑定的ip地址
        List<String> ipList = stringRedisTemplate.opsForValue().multiGet(cacheKeyList);
        Map<String, List<Long>> userIdMap = new HashMap<>();
        ipList.forEach(ip -> {
            String currentIp = ip.substring(0, ip.indexOf("%"));
            Long userId = Long.valueOf(ip.substring(ip.indexOf("%") + 1));

            List<Long> currentUserIdList = userIdMap.getOrDefault(currentIp, new ArrayList<>());
            currentUserIdList.add(userId);
            userIdMap.put(currentIp, currentUserIdList);
            LOGGER.info("batchSendMsg ip: " + currentIp);
        });
        //根据注册IP对ImMsgBody进行分组
        //将连接到同一台i地址的ImMsgBody组装到一个List中，进行统一发送
        Map<Long, ImMsgBody> userIdMsgMap = imMsgBodyList.stream().collect(Collectors.toMap(ImMsgBody::getUserId, x -> x));
        for (Map.Entry<String, List<Long>> entry : userIdMap.entrySet()) {
            //设置dubbo RPC上下文
            RpcContext.getContext().set("ip", entry.getKey());
            List<Long> currentUserIdList = entry.getValue();
            List<ImMsgBody> batchSendMsgBodyGroupByIpList = currentUserIdList.stream().map(userIdMsgMap::get).collect(Collectors.toList());
            routerHandlerRpc.batchSendMsg(batchSendMsgBodyGroupByIpList);
        }
    }
}
