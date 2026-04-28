package org.xy.live.account.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.account.interfaces.IAccountTokenRPC;
import org.xy.live.account.provider.service.AccountTokenService;

@DubboService
public class IAccountTokenRPCImpl implements IAccountTokenRPC {

    @Resource
    private AccountTokenService accountTokenService;

    @Override
    public String createAndSaveLoginToken(Long userId) {
        return accountTokenService.createAndSaveLoginToken(userId);
    }

    @Override
    public Long getUserIdByToken(String tokenKey) {
        return accountTokenService.getUserIdByToken(tokenKey);
    }
}
