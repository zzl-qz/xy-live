package org.zzl.xy.live.api.service;

import org.zzl.xy.live.api.vo.LivingRoomInitVO;
import org.zzl.xy.live.api.vo.req.OnlinePKReqVO;
import org.zzl.xy.live.api.vo.resp.LivingRoomPageRespVO;
import org.zzl.xy.live.api.vo.req.LivingRoomReqVO;
import org.zzl.xy.live.api.vo.resp.RedPacketReceiveVO;

public interface ILivingRoomService {
    
    /**
     * 开始直播
     */
    Integer startingLiving(Integer type);

    /**
     * 关闭直播
     */
    boolean closeLiving(Integer roomId);

    /**
     * 根据当前用户（观看者）Id和直播间Id查询当前用户（观看者）的信息、主播的信息以及目标直播间的信息封装返回给前端
     */
    LivingRoomInitVO anchorConfig(Long userId, Integer roomId);


    /**
     * 查询直播间列表（分页）
     */
    LivingRoomPageRespVO list(LivingRoomReqVO livingRoomReqVO);

    /**
     * 当PK直播间连上线准备PK时，调用该请求
     */
    boolean onlinePk(OnlinePKReqVO onlinePKReqVO);

    /**
     * 主播初始化红包雨
     * @param userId
     * @param roomId
     * @return
     */
    boolean prepareRedPacket(Long userId, Integer roomId);

    /**
     * 主播点击开启红包雨
     * @param userId
     * @param redPacketConfigCode
     * @return
     */
    boolean startRedPacket(Long userId, String redPacketConfigCode);

    /**
     * 直播间用户点击获取红包（抢红包）
     * @param userId
     * @param redPacketConfigCode
     * @return
     */
    RedPacketReceiveVO getRedPacket(Long userId, String redPacketConfigCode);
}