package org.xy.live.living.provider.service;

import org.xy.live.common.interfaces.dto.PageWrapper;
import org.xy.live.im.core.server.interfaces.dto.ImOfflineDTO;
import org.xy.live.im.core.server.interfaces.dto.ImOnlineDTO;
import org.xy.live.living.interfaces.dto.LivingPkRespDTO;
import org.xy.live.living.interfaces.dto.LivingRoomReqDTO;
import org.xy.live.living.interfaces.dto.LivingRoomRespDTO;

import java.util.List;

public interface ILivingRoomService {

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
     * 根据直播间id查询直播间详细信息
     *
     * @param roomId
     * @return
     */
    LivingRoomRespDTO queryByRoomId(Integer roomId);

    /**
     * 查询直播间列表（分页）
     *
     * @param livingRoomReqDTO
     * @return
     */
    PageWrapper<LivingRoomRespDTO> list(LivingRoomReqDTO livingRoomReqDTO);

    /**
     * 从数据库中查询最新的1000条直播间列表信息
     *
     * @param type
     * @return
     */
    List<LivingRoomRespDTO> listAllLivingRoomFromDB(Integer type);

    /**
     * 用户上线userId roomId绑定处理
     * @param imOnlineDTO
     */
    void userOnlineHandler(ImOnlineDTO imOnlineDTO);

    /**
     * 用户下线userId roomId解绑处理
     * @param imOfflineDTO
     */
    void userOfflineHandler(ImOfflineDTO imOfflineDTO);

    /**
     * 支持根据roomId查询出批量的userId（set）存储
     */
    List<Long> queryUserIdsByRoomId(LivingRoomReqDTO livingRoomReqDTO);

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
     * 用户在pk直播间中，连上线请求
     *
     * @param livingRoomReqDTO
     * @return
     */
    LivingPkRespDTO onlinePk(LivingRoomReqDTO livingRoomReqDTO);

    /**
     * 根据主播Id查询直播间信息
     * @param anchorId
     * @return
     */
    LivingRoomRespDTO queryByAnchorId(Long anchorId);
}