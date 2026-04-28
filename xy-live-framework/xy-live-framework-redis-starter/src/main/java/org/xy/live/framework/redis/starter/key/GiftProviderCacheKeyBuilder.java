package org.xy.live.framework.redis.starter.key;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(RedisKeyLoadMatch.class)
public class GiftProviderCacheKeyBuilder extends RedisKeyBuilder {

    private static String GIFT_CONFIG_CACHE = "gift_config_cache";
    private static String GIFT_LIST_CACHE = "gift_list_cache";
    private static String GIFT_CONSUME_KEY = "gift_consume_key";
    private static String GIFT_LIST_LOCK = "gift_list_lock";
    private static String LIVING_PK_KEY = "living_pk_key";
    private static String LIVING_PK_SEND_SEQ = "living_pk_send_seq";
    private static String LIVING_PK_IS_OVER = "living_pk_is_over";
    private static String RED_PACKET_LIST = "red_packet_list";
    private static String RED_PACKET_INIT_LOCK = "red_packet_init_lock";
    private static String RED_PACKET_TOTAL_GET_COUNT = "red_packet_total_get_count";
    private static String RED_PACKET_TOTAL_GET_PRICE = "red_packet_total_get_price";
    private static String RED_PACKET_MAX_GET_PRICE = "red_packet_max_get_price";
    private static String USER_TOTAL_GET_PRICE_CACHE = "red_packet_user_total_get_price";
    private static String RED_PACKET_PREPARE_SUCCESS = "red_packet_prepare_success";
    private static String RED_PACKET_NOTIFY = "red_packet_notify";
    private static String GET_RED_PACKET_PRICE = "get_red_packet_price"; // 在这个红包雨中获取到的金额

    private static String RED_PACKET_MAX_PRICE = "red_packet_max_price"; // 最大的红包金额数量

    private static String SKU_DETAIL_INFO_MAP = "sku_detail_info_map";
    private static String SHOP_CAR = "shop_car";
    private static String SKU_STOCK = "sku_stock";
    private static String STOCK_SYNC_LOCK = "stock_sync_lock";
    private static String ORDER_BY_USER_LOOM = "order_by_user_room";
    private static String ORDER_BY_USER = "order_by_user";
    private static String SYNC_DB_STOCK = "sync_db_stock";
    private static String PREPARE_SKU_STOCK_FLAG = "prepare_sku_stock_flag";
    private static String SYNC_RED_PACK_DATA_FLAG = "sync_red_pack_data_flag";
    private static String SYNC_RED_PACK_DATA_LOCK = "sync_red_pack_data_lock";
    private static String PREPARE_ORDER_LOCK = "prepare_order_lock";
    private static String PAY_ORDER_LOCK = "pay_order_lock";

    public String buildOrderByUserIdAndRoomId(Long userId, Integer roomId) {
        return super.getPrefix() + ORDER_BY_USER_LOOM + super.getSplitItem() + roomId;
    }

    public String buildOrderByUserId(Long userId) {
        return super.getPrefix() + ORDER_BY_USER;
    }

    public String buildStockSyncLock() {
        return super.getPrefix() + STOCK_SYNC_LOCK;
    }

    public String buildSkuStock(Long skuId) {
        return super.getPrefix() + SKU_STOCK + super.getSplitItem() + skuId;
    }

    public String buildUserShopCar(Long userId) {
        return super.getPrefix() + SHOP_CAR + super.getSplitItem() + userId;
    }

    public String buildUserShopCar_v2(Long userId, Integer roomId) {
        return super.getPrefix() + SHOP_CAR + super.getSplitItem() + userId + super.getSplitItem() + roomId;
    }

    public String buildSkuDetailInfoMap(Long anchorId) {
        return super.getPrefix() + SKU_DETAIL_INFO_MAP + super.getSplitItem() + anchorId;
    }

    public String buildLivingPkIsOver(Integer roomId) {
        return super.getPrefix() + LIVING_PK_IS_OVER + super.getSplitItem() + roomId;
    }

    public String buildLivingPkSendSeq(Integer roomId) {
        return super.getPrefix() + LIVING_PK_SEND_SEQ + super.getSplitItem() + roomId;
    }

    public String buildLivingPkKey(Integer roomId) {
        return super.getPrefix() + LIVING_PK_KEY + super.getSplitItem() + roomId;
    }

    public String buildGiftConsumeKey(String uuid) {
        return super.getPrefix() + GIFT_CONSUME_KEY + super.getSplitItem() + uuid;
    }

    public String buildGiftConfigCacheKey(int giftId) {
        return super.getPrefix() + GIFT_CONFIG_CACHE + super.getSplitItem() + giftId;
    }

    public String buildGiftListCacheKey() {
        return super.getPrefix() + GIFT_LIST_CACHE;
    }

    public String buildGiftListLockCacheKey() {
        return super.getPrefix() + GIFT_LIST_LOCK;
    }

    public String buildRedPacketList(String code) {
        return super.getPrefix() + RED_PACKET_LIST + super.getSplitItem() + code;
    }

    public String buildRedPacketInitLock(String code) {
        return super.getPrefix() + RED_PACKET_INIT_LOCK + super.getSplitItem() + code;
    }

    public String buildRedPacketTotalGetCount(String code) {
        return super.getPrefix() + RED_PACKET_TOTAL_GET_COUNT + super.getSplitItem() + (Math.abs(code.hashCode()) % 100);
    }

    public String buildRedPacketTotalGetPrice(String code) {
        return super.getPrefix() + RED_PACKET_TOTAL_GET_PRICE + super.getSplitItem() + (Math.abs(code.hashCode()) % 100);
    }

    public String buildRedPacketMaxGetPrice(String code) {
        return super.getPrefix() + RED_PACKET_MAX_GET_PRICE + super.getSplitItem() + (Math.abs(code.hashCode()) % 100);
    }

    public String buildUserTotalGetPrice(Long userId) {
        return super.getPrefix() + USER_TOTAL_GET_PRICE_CACHE + super.getSplitItem() + userId;
    }

    public String buildRedPacketPrepareSuccess(String code) {
        return super.getPrefix() + RED_PACKET_PREPARE_SUCCESS + super.getSplitItem() + code;
    }

    public String buildRedPacketNotify(String code) {
        return super.getPrefix() + RED_PACKET_NOTIFY + super.getSplitItem() + code;
    }
    public String buildGetRedPacketPrice(Long userId, String code) {
        return super.getPrefix() + GET_RED_PACKET_PRICE + super.getSplitItem() + userId + super.getSplitItem() + code;
    }

    public String buildRedPacketMaxPrice(String code) {
        return super.getPrefix() + RED_PACKET_MAX_PRICE + super.getSplitItem() + code;
    }

    public String buildSyncDBStock(String msgId) {
        return super.getPrefix() + SYNC_DB_STOCK + super.getSplitItem() + msgId;
    }

    public String buildPrepareSkuStockFlag(Long anchorId) {
        return super.getPrefix() + PREPARE_SKU_STOCK_FLAG + super.getSplitItem() + anchorId;
    }

    public String buildSyncRedPackDataFlag(String code) {
        return super.getPrefix() + SYNC_RED_PACK_DATA_FLAG + super.getSplitItem() + code;
    }

    public String buildSyncRedPackDataLock(String code) {
        return super.getPrefix() + SYNC_RED_PACK_DATA_LOCK + super.getSplitItem() + code;
    }

    public String buildPrepareOrderLock(Long userId, Integer roomId) {
        return super.getPrefix() + PREPARE_ORDER_LOCK + super.getSplitItem() + userId + super.getSplitItem() + roomId;
    }

    public String buildPayOrderLock(Long userId, Long orderId) {
        return super.getPrefix() + PAY_ORDER_LOCK + super.getSplitItem() + userId + super.getSplitItem() + orderId;
    }
}