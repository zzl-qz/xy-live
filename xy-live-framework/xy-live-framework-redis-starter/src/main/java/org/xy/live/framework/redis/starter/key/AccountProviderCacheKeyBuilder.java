package org.xy.live.framework.redis.starter.key;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Conditional;

@Configurable
@Conditional(RedisKeyLoadMatch.class)
public class AccountProviderCacheKeyBuilder extends RedisKeyBuilder{

    private static String ACCOUNT_TOKEN_KEY = "account";

    public String buildAccountTokenKey(String key) {
        return super.getPrefix() + ACCOUNT_TOKEN_KEY + super.getSplitItem() + key;
    }
}