package org.xy.live.framework.redis.starter.key;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Conditional;


/**
 * 缓存验证码的key
 */
@Configurable
@Conditional(RedisKeyLoadMatch.class)
public class MsgProviderCacheKeyBuilder extends RedisKeyBuilder{

    private static String SMS_LOGIN_CODE_KEY = "smsLoginCode";
    private static String MSG_UNION_MID_KEY = "msg_union_mid_key";

    public String buildSmsLoginCodeKey(String phone) {
        return super.getPrefix() + SMS_LOGIN_CODE_KEY + super.getSplitItem() + phone;
    }

    public String buildMsgUnionMidKey(String mid) {
        return super.getPrefix() + MSG_UNION_MID_KEY + super.getSplitItem() + mid;
    }

}