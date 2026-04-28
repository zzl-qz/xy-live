package org.xy.live.common.interfaces.topic;

public class ShopCarProviderTopicNames {

    /**
     * 购物车redis同步到MySQL消息topic
     */
    public static final String SYNC_MSG_TOPIC = "sync_msg_topic";

    /**
     * 清空购物车消息
     */
    public static final String CLEAR_CAR_MSG_TOPIC = "clear_car_msg_topic";

    /**
     * 修改最后访问时间消息
     */
    public static final String UPDATE_LAST_LIVE_AT_MSG_TOPIC = "update_last_live_at_msg_topic";

}
