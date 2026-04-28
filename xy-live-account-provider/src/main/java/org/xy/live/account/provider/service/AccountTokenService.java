package org.xy.live.account.provider.service;

/**
 * 账号token鉴权服务
 */
public interface AccountTokenService {

    /**
     * 创建一个登录token
     *
     * @param userId
     * @return
     */
    String createAndSaveLoginToken(Long userId);

    /**
     * 校验用户token
     *
     * @param tokenKey
     * @return
     */
    Long getUserIdByToken(String tokenKey);

}
