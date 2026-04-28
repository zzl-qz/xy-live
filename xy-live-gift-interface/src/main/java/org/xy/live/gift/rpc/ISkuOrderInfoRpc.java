package org.xy.live.gift.rpc;

import org.xy.live.gift.dto.PrepareOrderReqDTO;
import org.xy.live.gift.dto.SkuOrderInfoReqDTO;
import org.xy.live.gift.dto.SkuOrderInfoRespDTO;
import org.xy.live.gift.dto.SkuPrepareOrderInfoDTO;

import java.util.List;

public interface ISkuOrderInfoRpc {

    /**
     * 根据userId和roomId查询订单信息
     */
    List<SkuOrderInfoRespDTO> queryByUserIdAndRoomId(Long userId, Integer roomId);


    /**
     * 根据userId查询订单信息
     * @param userId
     * @return
     */

    List<SkuOrderInfoRespDTO> queryByUserId(Long userId);

    /**
     * 插入一条订单
     */
    boolean insertOne(SkuOrderInfoReqDTO skuOrderInfoReqDTO);



    /**
     * 更新订单状态
     */
    boolean updateOrderStatus(SkuOrderInfoReqDTO skuOrderInfoReqDTO, Integer oldTypeCode);

    /**
     * 预支付订单生成
     */
    SkuPrepareOrderInfoDTO prepareOrder(PrepareOrderReqDTO reqDTO);

    /**
     * 订单支付(这个接口是用户在直播间中进行支付)
     * @param userId
     * @param roomId
     * @return
     */
    boolean payNow(Long userId, Integer roomId);


    /**
     * 订单支付（这个接口是给到全局购物车的）
     * @param orderId
     * @return
     */
    boolean payNowByOrderId(Long orderId);
}