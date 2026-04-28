package org.xy.live.bank.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.bank.interfaces.dto.PayOrderDTO;
import org.xy.live.bank.interfaces.rpc.IPayOrderRpc;
import org.xy.live.bank.provider.dao.po.PayOrderPO;
import org.xy.live.bank.provider.service.PayOrderService;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;

@DubboService
public class PayOrderRpcImpl implements IPayOrderRpc {
    
    @Resource
    private PayOrderService payOrderService;
    
    @Override
    public String insertOne(PayOrderDTO payOrderDTO) {
        System.out.println(ConvertBeanUtils.convert(payOrderDTO, PayOrderPO.class));
        return payOrderService.insertOne(ConvertBeanUtils.convert(payOrderDTO, PayOrderPO.class));
    }

    @Override
    public boolean updateOrderStatus(Long id, Integer status) {
        return payOrderService.updateOrderStatus(id, status);
    }

    @Override
    public boolean updateOrderStatus(String orderId, Integer status) {
        return payOrderService.updateOrderStatus(orderId, status);
    }

    @Override
    public boolean payNotify(PayOrderDTO payOrderDTO) {
        return payOrderService.payNotify(payOrderDTO);
    }
}