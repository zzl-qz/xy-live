package org.zzl.xy.live.api.service.impl;

import cn.hutool.core.bean.BeanUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xy.live.common.interfaces.dto.PageWrapper;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.gift.dto.RedPacketConfigReqDTO;
import org.xy.live.gift.dto.RedPacketConfigRespDTO;
import org.xy.live.gift.dto.RedPacketReceiveDTO;
import org.xy.live.gift.rpc.IRedPacketConfigRpc;
import org.xy.live.im.constants.AppIdEnum;
import org.xy.live.living.interfaces.dto.LivingPkRespDTO;
import org.xy.live.living.interfaces.dto.LivingRoomReqDTO;
import org.xy.live.living.interfaces.dto.LivingRoomRespDTO;
import org.xy.live.living.interfaces.rpc.ILivingRoomRpc;
import org.xy.live.user.dto.UserDTO;
import org.xy.live.user.interfaces.rpc.IUserRpc;
import org.xy.live.user.interfaces.rpc.IUserTagRpc;
import org.xy.live.web.starter.context.XyRequestContext;
import org.xy.live.web.starter.error.BizBaseErrorEnum;
import org.xy.live.web.starter.error.ErrorAssert;
import org.zzl.xy.live.api.error.XyApiError;
import org.zzl.xy.live.api.service.ILivingRoomService;
import org.zzl.xy.live.api.vo.LivingRoomInitVO;
import org.zzl.xy.live.api.vo.req.LivingRoomReqVO;
import org.zzl.xy.live.api.vo.req.OnlinePKReqVO;
import org.zzl.xy.live.api.vo.resp.LivingRoomPageRespVO;
import org.zzl.xy.live.api.vo.resp.LivingRoomRespVO;
import org.zzl.xy.live.api.vo.resp.RedPacketReceiveVO;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.xy.live.user.constants.UserTagsEnum.IS_VIP;

@Service
public class LivingRoomServiceImpl implements ILivingRoomService {


    private static final Logger LOGGER = LoggerFactory.getLogger(LivingRoomServiceImpl.class);

    @DubboReference(timeout = 15000)
    private ILivingRoomRpc livingRoomRpc;
    @DubboReference(timeout = 15000)
    private IUserRpc userRpc;

    @DubboReference(timeout = 15000)
    private IUserTagRpc userTagRpc;

    @DubboReference(timeout = 15000)
    private IRedPacketConfigRpc redPacketConfigRpc;


    @Override
    public Integer startingLiving(Integer type) {
        // 拿一下用户信息
        Long userId = XyRequestContext.getUserId();
        UserDTO userDTO = userRpc.getByUserId(userId);
        ErrorAssert.isNotNull(userDTO, XyApiError.NO_USER_EXIT);

        // 判断一下是否是主播，不是主播的话无权开播
        boolean isAnchor = userTagRpc.containTag(userId, IS_VIP);
        ErrorAssert.isTure(isAnchor, XyApiError.NO_ANCHOR);

        // 封装一下直播间信息
        LivingRoomReqDTO livingRoomReqDTO = new LivingRoomReqDTO();
        livingRoomReqDTO.setAnchorId(userId);
        livingRoomReqDTO.setRoomName("主播-" + userId + "的直播间");
        livingRoomReqDTO.setCovertImg(userDTO.getAvatar());
        livingRoomReqDTO.setType(type);
        return livingRoomRpc.startLivingRoom(livingRoomReqDTO);
    }

    @Override
    public boolean closeLiving(Integer roomId) {
        LivingRoomReqDTO livingRoomReqDTO = new LivingRoomReqDTO();
        livingRoomReqDTO.setRoomId(roomId);
        livingRoomReqDTO.setAnchorId(XyRequestContext.getUserId());
        boolean closed = livingRoomRpc.closeLiving(livingRoomReqDTO); // 关闭直播间
        LOGGER.info("关播状态" + closed);
        return closed;
    }

    @Override
    public LivingRoomPageRespVO list(LivingRoomReqVO livingRoomReqVO) {
        // 查询查询直播间列表记录
        PageWrapper<LivingRoomRespDTO> resultPage = livingRoomRpc.list(BeanUtil.copyProperties(livingRoomReqVO, LivingRoomReqDTO.class));

        // 封装返回数据类型VO
        LivingRoomPageRespVO livingRoomPageRespVO = new LivingRoomPageRespVO();
        livingRoomPageRespVO.setList(ConvertBeanUtils.convertList(resultPage.getList(), LivingRoomRespVO.class));
        livingRoomPageRespVO.setHasNext(resultPage.isHasNext());

        return livingRoomPageRespVO;
    }

    @Override
    public boolean onlinePk(OnlinePKReqVO onlinePKReqVO) {
        // 封装直播间信息DTO
        LivingRoomReqDTO reqDTO = new LivingRoomReqDTO();
        reqDTO.setRoomId(onlinePKReqVO.getRoomId());
        reqDTO.setAppId(AppIdEnum.XY_LIVE_BIZ.getCode());
        reqDTO.setPkObjId(XyRequestContext.getUserId());
        LivingPkRespDTO tryOnlineStatus = livingRoomRpc.onlinePk(reqDTO);

        // 如果当前直播间处于连线中，直接抛出连线异常
        ErrorAssert.isTure(tryOnlineStatus.isOnlineStatus(), XyApiError.PK_ONLINE_BUSY);

        return true;
    }


    /**
     * 根据当前用户（观看者）Id和直播间Id查询当前用户（观看者）的信息、主播的信息以及目标直播间的信息封装返回给前端
     * @param userId
     * @param roomId
     * @return
     */
    @Override
    public LivingRoomInitVO anchorConfig(Long userId, Integer roomId) {
        // 根据直播间查询直播间的详细信息
        LivingRoomRespDTO respDTO = livingRoomRpc.queryByRoomId(roomId);

        // 将主播Id和当前观看者Id封装成一个集合，并且进行了去重操作，避免两个Id是相同的，导致执行了两次相同的查询浪费性能问题
        Map<Long, UserDTO> userDTOMap = userRpc.batchQueryUserInfo(Arrays.asList(respDTO.getAnchorId(), userId)
                .stream().distinct().collect(Collectors.toList()));
        UserDTO anchor = userDTOMap.get(respDTO.getAnchorId());
        UserDTO watcher = userDTOMap.get(userId);

        // 封装直播间信息以及用户信息和当前观看者信息VO对象
        LivingRoomInitVO respVO = new LivingRoomInitVO();
        respVO.setAnchorNickName(anchor.getNickName());
        respVO.setWatcherNickName(watcher.getNickName());
        respVO.setNickName(watcher.getNickName());
        respVO.setUserId(userId);
        respVO.setAvatar(StringUtils.isEmpty(anchor.getAvatar()) ?
                "https://s1.ax1x.com/2022/12/18/zb6q6f.png" : anchor.getAvatar());
        respVO.setWatcherAvatar(StringUtils.isEmpty(watcher.getAvatar()) ?
                "https://s1.ax1x.com/2022/12/18/zb6q6f.png" : watcher.getAvatar());
        respVO.setWatcherAvatar(watcher.getAvatar());
        if (respDTO.getAnchorId() == null || userId == null) { //直播间不存在，设置roomId为-1
            respVO.setRoomId(-1);
            return respVO;
        }
        boolean isAnchor = respDTO.getAnchorId().equals(userId); // 判断是否是主播本人
        respVO.setRoomId(respDTO.getId());
        respVO.setAnchorId(respDTO.getAnchorId());
        respVO.setAnchor(isAnchor); // 是否是主播本人
        if (isAnchor) { // 是主播本人的话，就判断一下是否有红包雨配置
            RedPacketConfigRespDTO redPacketConfigRespDTO = redPacketConfigRpc.queryByAnchorId(userId);
            if (redPacketConfigRespDTO != null && redPacketConfigRespDTO.getConfigCode() != null) {
                respVO.setRedPacketConfigCode(redPacketConfigRespDTO.getConfigCode());
            }
        }
        respVO.setDefaultBgImg("http://www.news.cn/photo/20250425/e2f1fb2690d74d099f6b5478be683fcc/ad07d300366b41b29df095f5f8946217.jpg");

        return respVO;
    }


    @Override
    public boolean prepareRedPacket(Long userId, Integer roomId) {
        // 直播间是否存在
        LivingRoomRespDTO livingRoomRespDTO = livingRoomRpc.queryByRoomId(roomId);
        ErrorAssert.isNotNull(livingRoomRespDTO, BizBaseErrorEnum.PARAM_ERROR);

        // 是否是主播本人点击
        Long anchorId = livingRoomRespDTO.getAnchorId();
        ErrorAssert.isTure(anchorId.equals(userId), BizBaseErrorEnum.PARAM_ERROR);

        // 开始点击初始化
        return redPacketConfigRpc.prepareRedPacket(userId);
    }


    @Override
    public boolean startRedPacket(Long userId, String code) {
        RedPacketConfigReqDTO reqDTO = new RedPacketConfigReqDTO();
        reqDTO.setUserId(userId);
        reqDTO.setRedPacketConfigCode(code);
        LivingRoomRespDTO livingRoomRespDTO = livingRoomRpc.queryByAnchorId(userId);
        ErrorAssert.isNotNull(livingRoomRespDTO, BizBaseErrorEnum.PARAM_ERROR);
        reqDTO.setRoomId(livingRoomRespDTO.getId());
        return redPacketConfigRpc.startRedPacket(reqDTO);
    }

    @Override
    public RedPacketReceiveVO getRedPacket(Long userId, String redPacketConfigCode) {
        RedPacketConfigReqDTO reqDTO = new RedPacketConfigReqDTO();
        reqDTO.setUserId(userId);
        reqDTO.setRedPacketConfigCode(redPacketConfigCode);
        RedPacketReceiveDTO receiveDTO = redPacketConfigRpc.receiveRedPacket(reqDTO);
        RedPacketReceiveVO respVO = new RedPacketReceiveVO();
        if (receiveDTO == null) {
            respVO.setMsg("红包已派发完毕");
        } else {
            respVO.setPrice(receiveDTO.getPrice());
            respVO.setMsg(receiveDTO.getNotifyMsg());
        }
        return respVO;
    }

}