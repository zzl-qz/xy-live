package org.xy.live.bank.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.bank.interfaces.dto.XyCurrencyTradeDTO;
import org.xy.live.bank.interfaces.rpc.XyCurrencyTradeRpc;
import org.xy.live.bank.provider.service.XyCurrencyTradeService;

/**
 * @Author: Daylight
 * @Date: 2026-01-29 21:17
 */
@DubboService
public class XyCurrencyTradeRpcImpl implements XyCurrencyTradeRpc {

    @Resource
    private XyCurrencyTradeService xyCurrencyTradeService;
    @Override
    public XyCurrencyTradeDTO getByExtraCol(int code, String extraCol) {
        return xyCurrencyTradeService.getByExtraCol(code, extraCol);
    }
}
