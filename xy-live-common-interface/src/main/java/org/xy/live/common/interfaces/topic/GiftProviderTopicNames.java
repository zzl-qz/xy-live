package org.xy.live.common.interfaces.topic;

public class GiftProviderTopicNames {

    /**
     * 移除礼物信息的缓存
     */
    public static final String REMOVE_GIFT_CACHE = "remove_gift_cache";

    /**
     * 发送礼物消息
     */
    public static final String SEND_GIFT = "send_gift";

    /**
     * 用户抢红包之后金额增长
     */
    public static final String ADD_RED_PACKET_PRICE = "add_red_packet_price";

    /**
     * 用户抢红包处理
     */
    public static final String RECEIVE_RED_PACKET_HANDLER = "receive_red_packet_handler";

    /**
     * 回滚未支付订单库存的topic
     */
    public static final String ROLL_BACK_STOCK = "roll_back_stock";

    /**
     * 开启直播时同步商品库存到Redis中的topic
     */
    public static final String START_LIVING_ROOM = "start_living_room";

    /**
     * 发送同步红包雨活动数据topic
     */
    public static final String SYNC_RED_PACK_DATA_TOPIC = "sync_red_pack_data_topic";
}

