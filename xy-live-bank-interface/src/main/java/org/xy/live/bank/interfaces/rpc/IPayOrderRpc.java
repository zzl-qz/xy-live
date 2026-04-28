package org.xy.live.bank.interfaces.rpc;

import org.xy.live.bank.interfaces.dto.PayOrderDTO;

public interface IPayOrderRpc {

    /**
     *插入订单 ，返回orderId
     */
    String insertOne(PayOrderDTO payOrderDTO);


    /**
     * 根据主键id更新订单状态
     */
    boolean updateOrderStatus(Long id, Integer status);


    /**
     * 更新订单状态
     */
    boolean updateOrderStatus(String orderId, Integer status);

    /**
     * 微信支付回调
     * @param payOrderDTO
     * @return
     */
    boolean payNotify(PayOrderDTO payOrderDTO);
}