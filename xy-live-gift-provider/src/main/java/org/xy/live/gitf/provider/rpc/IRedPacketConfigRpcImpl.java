package org.xy.live.gitf.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.gift.dto.RedPacketConfigReqDTO;
import org.xy.live.gift.dto.RedPacketConfigRespDTO;
import org.xy.live.gift.dto.RedPacketReceiveDTO;
import org.xy.live.gift.rpc.IRedPacketConfigRpc;
import org.xy.live.gitf.provider.dao.po.RedPacketConfigPO;
import org.xy.live.gitf.provider.service.IRedPacketConfigService;

@DubboService
public class IRedPacketConfigRpcImpl implements IRedPacketConfigRpc {

    @Resource
    private IRedPacketConfigService redPacketConfigService;

    @Override
    public RedPacketConfigRespDTO queryByAnchorId(Long anchorId) {
        return ConvertBeanUtils.convert(redPacketConfigService.queryByAnchorId(anchorId), RedPacketConfigRespDTO.class);
    }

    @Override
    public boolean addOne(RedPacketConfigReqDTO redPacketConfigReqDTO) {
        return redPacketConfigService.addOne(ConvertBeanUtils.convert(redPacketConfigReqDTO, RedPacketConfigPO.class));
    }

    @Override
    public boolean prepareRedPacket(Long anchorId) {
        return redPacketConfigService.prepareRedPacket(anchorId);
    }

    @Override
    public RedPacketReceiveDTO receiveRedPacket(RedPacketConfigReqDTO redPacketConfigReqDTO) {
        return redPacketConfigService.receiveRedPacket(redPacketConfigReqDTO);
    }

    @Override
    public Boolean startRedPacket(RedPacketConfigReqDTO reqDTO) {
        return redPacketConfigService.startRedPacket(reqDTO);
    }
}
