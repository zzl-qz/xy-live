package org.xy.live.im.core.server.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.im.core.server.interfaces.rpc.IRouterHandlerRpc;
import org.xy.live.im.core.server.service.IRouterHandlerService;
import org.xy.live.im.dto.ImMsgBody;

import java.sql.SQLOutput;
import java.util.List;

@DubboService
public class IRouterHandlerRpcImpl implements IRouterHandlerRpc {

    @Resource
    private IRouterHandlerService routerHandlerService;


    /**
     * 按照用户id进行消息的发送
     *
     * @param imMsgBody
     */
    @Override
    public void sendMsg(ImMsgBody imMsgBody) {
        routerHandlerService.onReceive(imMsgBody);
    }

    /**
     * 批量推送消息
     * @param imMsgBodyList
     */
    @Override
    public void batchSendMsg(List<ImMsgBody> imMsgBodyList) {
        imMsgBodyList.forEach(imMsgBody -> {
            routerHandlerService.onReceive(imMsgBody);
        });
    }

}
