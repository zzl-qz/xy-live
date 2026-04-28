package org.xy.live.im.interfaces;

/**
 * 判断用户是否在线
 */
public interface ImOnlineRpc {

    /**
     * 判断用户是否在线
     *
     * @param userId
     * @param appId
     * @return
     */
    boolean isOnline(long userId, int appId);

}
