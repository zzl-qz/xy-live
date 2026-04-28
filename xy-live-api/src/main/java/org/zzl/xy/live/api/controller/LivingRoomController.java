package org.zzl.xy.live.api.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;
import org.xy.live.common.interfaces.vo.WebResponseVO;
import org.xy.live.web.starter.context.XyRequestContext;
import org.xy.live.web.starter.error.BizBaseErrorEnum;
import org.xy.live.web.starter.error.ErrorAssert;
import org.xy.live.web.starter.limit.RequestLimit;
import org.zzl.xy.live.api.error.XyApiError;
import org.zzl.xy.live.api.service.ILivingRoomService;
import org.zzl.xy.live.api.vo.LivingRoomInitVO;
import org.zzl.xy.live.api.vo.req.LivingRoomReqVO;
import org.zzl.xy.live.api.vo.req.OnlinePKReqVO;
import org.zzl.xy.live.api.vo.resp.LivingRoomPageRespVO;
import org.zzl.xy.live.api.vo.resp.RedPacketReceiveVO;

@RestController
@RequestMapping("/living")
@Slf4j
public class LivingRoomController {

    @Resource
    private ILivingRoomService livingRoomService;

    /**
     * 开播
     * @param type
     * @return
     */
    @RequestLimit(limit = 1, second = 10, msg = "开播请求过于频繁，请稍后再试")
    @PostMapping("/startingLiving")
    public WebResponseVO<LivingRoomInitVO> startingLiving(Integer type) {
        if (type == null) {
            return WebResponseVO.errorParam("需要给定直播间类型");
        }
        Integer roomId = livingRoomService.startingLiving(type);
        LivingRoomInitVO livingRoomInitVO = new LivingRoomInitVO();
        livingRoomInitVO.setRoomId(roomId);
        return WebResponseVO.success(livingRoomInitVO);
    }


    /**
     * 用来接收在挑战者请求参与到pk活动中的上线请求
     * 当用户想要参与到pk的话，就需要请求到这个接口
     * @param onlinePKReqVO
     * @return
     */
    @PostMapping("/onlinePK")
    @RequestLimit(limit = 1, second = 3)
    public WebResponseVO<Boolean> onlinePk(@RequestBody OnlinePKReqVO onlinePKReqVO) {
        ErrorAssert.isNotNull(onlinePKReqVO.getRoomId(), BizBaseErrorEnum.PARAM_ERROR);
        return WebResponseVO.success(livingRoomService.onlinePk(onlinePKReqVO));
    }


    /**
     * 关播
     * @param roomId
     * @return
     */
    @RequestLimit(limit = 1, second = 10, msg = "关播请求过于频繁，请稍后再试")
    @PostMapping("/closeLiving")
    public WebResponseVO<Object> closeLiving(Integer roomId) {
        ErrorAssert.isNotNull(roomId, BizBaseErrorEnum.PARAM_ERROR);
        boolean status = livingRoomService.closeLiving(roomId);
        if (status) {
            return WebResponseVO.success();
        }
        return WebResponseVO.bizError("关播异常，请稍后再试");
    }

    /**
     * 根据当前用户（观看者）Id和直播间Id查询当前用户（观看者）的信息、主播的信息以及目标直播间的信息封装返回给前端
     * @param roomId
     * @return
     */
    @PostMapping("/anchorConfig")
    public WebResponseVO<LivingRoomInitVO> anchorConfig(Integer roomId) {
        Long userId = XyRequestContext.getUserId();
        return WebResponseVO.success(livingRoomService.anchorConfig(userId, roomId));
    }

    /**
     * 分页查询直播间列表
     * @param livingRoomReqVO
     * @return
     */
    @PostMapping("/list")
    public WebResponseVO<LivingRoomPageRespVO> list(@RequestBody LivingRoomReqVO livingRoomReqVO) {
        ErrorAssert.isTure(livingRoomReqVO != null || livingRoomReqVO.getType() != null, XyApiError.LIVING_ROOM_TYPE_MISSING);
        ErrorAssert.isTure(!(livingRoomReqVO.getPage() <= 0 || livingRoomReqVO.getPageSize() > 100), BizBaseErrorEnum.PARAM_ERROR);
        return WebResponseVO.success(livingRoomService.list(livingRoomReqVO));
    }

    /**
     * 主播初始化红包雨
     * @param livingRoomReqVO
     * @return
     */
    @RequestLimit(limit = 1, second = 10, msg = "正在初始化红包数据，请稍等")
    @PostMapping("/prepareRedPacket")
    public WebResponseVO<Boolean> prepareRedPacket(@RequestBody LivingRoomReqVO livingRoomReqVO) {
        return WebResponseVO.success(livingRoomService.prepareRedPacket(XyRequestContext.getUserId(), livingRoomReqVO.getRoomId()));
    }


    /**
     * 主播开启红包雨
     * @param livingRoomReqVO
     * @return
     */
    @RequestLimit(limit = 1, second = 10, msg = "正在广播直播间用户，请稍等")
    @PostMapping("/startRedPacket")
    public WebResponseVO<Boolean> startRedPacket(@RequestBody LivingRoomReqVO livingRoomReqVO) {
        return WebResponseVO.success(livingRoomService.startRedPacket(XyRequestContext.getUserId(), livingRoomReqVO.getRedPacketConfigCode()));
    }

    /**
     * 用户抢红包
     * @param livingRoomReqVO
     * @return
     */
    @RequestLimit(limit = 1, second = 3, msg = "") // 主要是为了给前端渲染特效的时间
    @PostMapping("/getRedPacket")
    public WebResponseVO<RedPacketReceiveVO> getRedPacket(@RequestBody LivingRoomReqVO livingRoomReqVO) {
        return WebResponseVO.success(livingRoomService.getRedPacket(XyRequestContext.getUserId(), livingRoomReqVO.getRedPacketConfigCode()));
    }
}