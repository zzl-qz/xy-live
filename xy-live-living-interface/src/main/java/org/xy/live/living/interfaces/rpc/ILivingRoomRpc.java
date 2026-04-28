package org.xy.live.living.interfaces.rpc;

import org.xy.live.common.interfaces.dto.PageWrapper;
import org.xy.live.living.interfaces.dto.LivingPkRespDTO;
import org.xy.live.living.interfaces.dto.LivingRoomReqDTO;
import org.xy.live.living.interfaces.dto.LivingRoomRespDTO;

import java.util.List;

public interface ILivingRoomRpc {

    /**
     * 开启直播间
     *
     * @param livingRoomReqDTO
     * @return
     */
    Integer startLivingRoom(LivingRoomReqDTO livingRoomReqDTO);

    /**
     * 关闭直播间
     *
     * @param livingRoomReqDTO
     * @return
     */
    boolean closeLiving(LivingRoomReqDTO livingRoomReqDTO);
    
    /**
     * 根据直播间id查询直播间的详细信息
     *
     * @param roomId
     * @return
     */
    LivingRoomRespDTO queryByRoomId(Integer roomId);

    /**
     * 直播间列表的分页查询
     *
     * @param livingRoomReqDTO
     * @return
     */
    PageWrapper<LivingRoomRespDTO> list(LivingRoomReqDTO livingRoomReqDTO);

    /**
     * 支持根据roomId查询出批量的userId（set）存储
     */
    List<Long> queryUserIdsByRoomId(LivingRoomReqDTO livingRoomReqDTO);

    /**
     * 用户在pk直播间中，连上线请求
     *
     * @param livingRoomReqDTO
     * @return
     */
    LivingPkRespDTO onlinePk(LivingRoomReqDTO livingRoomReqDTO);

    /**
     * 根据roomId查询当前pk人是谁
     *
     * @param roomId
     * @return
     */
    Long queryOnlinePkUserId(Integer roomId);

    /**
     * 用户在pk直播间下线
     *
     * @param livingRoomReqDTO
     * @return
     */
    boolean offlinePk(LivingRoomReqDTO livingRoomReqDTO);

    /**
     * 根据主播Id获取到当前直播间信息
     * @param anchorId
     * @return
     */
    LivingRoomRespDTO queryByAnchorId(Long anchorId);
}