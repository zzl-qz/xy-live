package org.xy.live.bank.api.service.impl;

import com.alibaba.fastjson2.JSON;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.xy.live.bank.api.service.IPayNotifyService;
import org.xy.live.bank.api.vo.WxPayNotifyVO;
import org.xy.live.bank.interfaces.dto.PayOrderDTO;
import org.xy.live.bank.interfaces.rpc.IPayOrderRpc;

@Service
public class PayNotifyServiceImpl implements IPayNotifyService {
    
    @DubboReference
    private IPayOrderRpc payOrderRpc;

    @Override
    public String notifyHandler(String paramJson) {
        WxPayNotifyVO wxPayNotifyVO = JSON.parseObject(paramJson, WxPayNotifyVO.class);
        PayOrderDTO payOrderDTO = new PayOrderDTO();
        payOrderDTO.setUserId(wxPayNotifyVO.getUserId());
        payOrderDTO.setOrderId(wxPayNotifyVO.getOrderId());
        payOrderDTO.setBizCode(wxPayNotifyVO.getBizCode());
        return payOrderRpc.payNotify(payOrderDTO) ? "success" : "fail";
    }
}