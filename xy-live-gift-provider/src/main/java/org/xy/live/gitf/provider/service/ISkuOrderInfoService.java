package org.xy.live.gitf.provider.service;

import org.xy.live.gift.constants.SkuOrderInfoEnum;
import org.xy.live.gift.dto.PrepareOrderReqDTO;
import org.xy.live.gift.dto.SkuOrderInfoReqDTO;
import org.xy.live.gift.dto.SkuOrderInfoRespDTO;
import org.xy.live.gitf.provider.dao.po.SkuOrderInfoPO;

import java.util.List;

public interface ISkuOrderInfoService {

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
    SkuOrderInfoPO insertOne(SkuOrderInfoReqDTO skuOrderInfoReqDTO);

    /**
     * 更新订单状态
     */
    boolean updateOrderStatus(SkuOrderInfoReqDTO skuOrderInfoReqDTO, Integer oldTypeCode);

    /**
     * 根据订单Id查询订单信息
     */
    SkuOrderInfoRespDTO queryByOrderId(Long orderId);

    /**
     * 判断是否是某个状态，如果是的话就返回true，否则返回false
     */
    boolean judgmentStatus(Long orderId, SkuOrderInfoEnum skuOrderInfoEnum);

    /**
     * 根据指定状态查询出一批
     * @param status
     * @return
     */
    List<SkuOrderInfoReqDTO> selectByStatus(int status);

}
