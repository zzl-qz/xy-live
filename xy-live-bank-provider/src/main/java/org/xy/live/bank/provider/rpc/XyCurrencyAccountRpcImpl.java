package org.xy.live.bank.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.common.interfaces.enums.TradeTypeEnum;
import org.xy.live.bank.interfaces.dto.AccountTradeReqDTO;
import org.xy.live.bank.interfaces.dto.AccountTradeRespDTO;
import org.xy.live.bank.interfaces.dto.XyCurrencyAccountDTO;
import org.xy.live.bank.interfaces.rpc.XyCurrencyAccountRpc;
import org.xy.live.bank.provider.service.XyCurrencyAccountService;

@DubboService
public class XyCurrencyAccountRpcImpl implements XyCurrencyAccountRpc {
    
    @Resource
    private XyCurrencyAccountService xyCurrencyAccountService;

    @Override
    public boolean insertOne(Long userId) {
        return xyCurrencyAccountService.insertOne(userId);
    }

    @Override
    public void incr(Long userId, int num, int code, String extraCol) {
        TradeTypeEnum typeEnum = TradeTypeEnum.getByCode(code);
        if (typeEnum == null) return;
        xyCurrencyAccountService.incr(userId, num, typeEnum, extraCol);
    }

    @Override
    public boolean decr(Long userId, int num, int code, String extraCol) {
        TradeTypeEnum typeEnum = TradeTypeEnum.getByCode(code);
        if (typeEnum == null) return false;
        return xyCurrencyAccountService.decr(userId, num, typeEnum, extraCol);
    }

    @Override
    public XyCurrencyAccountDTO getByUserId(Long userId) {
        return xyCurrencyAccountService.getByUserId(userId);
    }

    @Override
    public Integer getBalance(Long userId) {
        return xyCurrencyAccountService.getBalance(userId);
    }

    @Override
    public AccountTradeRespDTO consumeForSendGift(AccountTradeReqDTO accountTradeReqDTO) {
        return xyCurrencyAccountService.consumeForSendGift(accountTradeReqDTO);
    }
}