package org.xy.live.im.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.im.interfaces.ImOnlineRpc;
import org.xy.live.im.provider.service.ImOnlineService;

@DubboService
public class ImOnlineRpcImpl implements ImOnlineRpc {


    @Resource
    private ImOnlineService imOnlineService;

    /**
     * 判断用户是否在线
     *
     * @param userId
     * @param appId
     * @return
     */
    @Override
    public boolean isOnline(long userId, int appId) {
        return imOnlineService.isOnline(userId, appId);
    }
}
