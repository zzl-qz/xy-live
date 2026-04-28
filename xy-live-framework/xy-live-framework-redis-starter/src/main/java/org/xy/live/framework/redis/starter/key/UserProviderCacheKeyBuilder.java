package org.xy.live.framework.redis.starter.key;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Conditional;

@Configurable
@Conditional(RedisKeyLoadMatch.class)
public class UserProviderCacheKeyBuilder extends RedisKeyBuilder{

    private static String USER_INFO_KEY = "userInfo";
    private static String USER_TAG_KEY = "userTag";
    private static String USER_INFO_LOCK_KEY = "userInfoLock";
    private static String USER_TAG_LOCK_KEY = "userTagLock";
    private static String USER_TAG_DTO_LOCK_KEY = "userTagDTOLock";
    private static String USER_PHONE_OBJ_KEY = "userPhoneObj";
    private static String USER_PHONE_LIST_KEY = "userPhoneList";
    private static String USER_PHONE_LOCK_KEY = "userPhoneLock";
    private static String USER_LOGIN_TOKEN_KEY = "userLoginToken";

    public String buildUserInfoKey(Long userId) {
        return super.getPrefix() + USER_INFO_KEY + super.getSplitItem() + userId;
    }

    public String buildUserTagKey(Long userId) {
        return super.getPrefix() + USER_TAG_KEY + super.getSplitItem() + userId;
    }

    public String buildUserInfoLockKey(Long userId) {
        return super.getPrefix() + USER_INFO_LOCK_KEY + super.getSplitItem() + userId;
    }

    public String buildTagLockKey(Long userId) {
        return super.getPrefix() + USER_TAG_LOCK_KEY + super.getSplitItem() + userId;
    }

    public String buildGetUserTagDTOLockKey(Long userId) {
        return super.getPrefix() + USER_TAG_DTO_LOCK_KEY + super.getSplitItem() + userId;
    }

    public String buildPhoneObjKey(String phone) {
        return super.getPrefix() + USER_PHONE_OBJ_KEY + super.getSplitItem() + phone;
    }

    public String buildLoginTokenKey(String token) {
        return super.getPrefix() + USER_LOGIN_TOKEN_KEY + super.getSplitItem() + token;
    }

    public String buildUserPhoneLockKey(String phone) {
        return super.getPrefix() + USER_PHONE_LOCK_KEY + super.getSplitItem() + phone;
    }

    public String buildPhoneListKey(Long userId) {
        return super.getPrefix() + USER_PHONE_LIST_KEY + super.getSplitItem() + userId;
    }
}