package org.xy.live.bank.provider.service;


import org.xy.live.bank.interfaces.dto.XyOrderPayTradeDTO;

/**
 * 订单支付流水记录
 */
public interface XyOrderPayTradeService {

    /**
     * 插入一条流水记录
     *
     * @param userId
     * @param num
     * @param orderId
     * @param remark
     * @return
     */
    boolean insertOne(long userId, int num, String orderId, String remark);

    /**
     * 根据订单id进行查询
     * @param orderId
     * @return
     */
    XyOrderPayTradeDTO queryByOrderId(String orderId);


    /**
     * 删除记录
     * @param orderId
     * @return
     */
    boolean deleteByOrderId(String orderId);
}
