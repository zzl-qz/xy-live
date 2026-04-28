package org.xy.live.bank.provider.service;

import org.xy.live.bank.interfaces.dto.PayOrderDTO;
import org.xy.live.bank.provider.dao.po.PayOrderPO;

public interface PayOrderService {

    /**
     * 根据orderId查询订单信息
     */
    PayOrderPO queryByOrderId(String orderId);

    /**
     *插入订单 ，返回主键id
     */
    String insertOne(PayOrderPO payOrderPO);

    /**
     * 根据主键id更新订单状态
     */
    boolean updateOrderStatus(Long id, Integer status);

    /**
     * 更新订单状态
     */
    boolean updateOrderStatus(String orderId, Integer status);

    /**
     * 微信支付回调后处理逻辑
     * @param payOrderDTO
     * @return
     */
    boolean payNotify(PayOrderDTO payOrderDTO);
}
