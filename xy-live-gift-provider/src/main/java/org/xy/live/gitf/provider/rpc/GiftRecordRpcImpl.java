package org.xy.live.gitf.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.gift.dto.GiftRecordDTO;
import org.xy.live.gift.rpc.IGiftRecordRpc;
import org.xy.live.gitf.provider.service.IGiftConfigService;

@DubboService
public class GiftRecordRpcImpl implements IGiftRecordRpc {
    
    @Resource
    private IGiftConfigService giftService;

    @Override
    public void insertOne(GiftRecordDTO giftRecordDTO) {
        
    }
}