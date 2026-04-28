package org.xy.live.living.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.common.interfaces.dto.PageWrapper;
import org.xy.live.living.interfaces.dto.LivingPkRespDTO;
import org.xy.live.living.interfaces.dto.LivingRoomReqDTO;
import org.xy.live.living.interfaces.dto.LivingRoomRespDTO;
import org.xy.live.living.provider.service.ILivingRoomService;
import org.xy.live.living.interfaces.rpc.ILivingRoomRpc;

import java.util.List;

@DubboService
public class LivingRomRpcImpl implements ILivingRoomRpc {

    @Resource
    private ILivingRoomService livingRoomService;

    @Override
    public Integer startLivingRoom(LivingRoomReqDTO livingRoomReqDTO) {
        return livingRoomService.startLivingRoom(livingRoomReqDTO);
    }

    @Override
    public boolean closeLiving(LivingRoomReqDTO livingRoomReqDTO) {
        return livingRoomService.closeLiving(livingRoomReqDTO);
    }

    @Override
    public LivingRoomRespDTO queryByRoomId(Integer roomId) {
        return livingRoomService.queryByRoomId(roomId);
    }

    @Override
    public PageWrapper<LivingRoomRespDTO> list(LivingRoomReqDTO livingRoomReqDTO) {
        return livingRoomService.list(livingRoomReqDTO);
    }

    @Override
    public List<Long> queryUserIdsByRoomId(LivingRoomReqDTO livingRoomReqDTO) {
        return livingRoomService.queryUserIdsByRoomId(livingRoomReqDTO);
    }

    @Override
    public LivingPkRespDTO onlinePk(LivingRoomReqDTO livingRoomReqDTO) {
        return livingRoomService.onlinePk(livingRoomReqDTO);
    }

    @Override
    public Long queryOnlinePkUserId(Integer roomId) {
        return livingRoomService.queryOnlinePkUserId(roomId);
    }

    @Override
    public boolean offlinePk(LivingRoomReqDTO offlinePk) {
        return livingRoomService.offlinePk(offlinePk);
    }

    @Override
    public LivingRoomRespDTO queryByAnchorId(Long anchorId) {
        return livingRoomService.queryByAnchorId(anchorId);
    }


}