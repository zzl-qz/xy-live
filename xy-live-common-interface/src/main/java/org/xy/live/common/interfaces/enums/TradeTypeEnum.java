package org.xy.live.common.interfaces.enums;

/**
 * 流水记录类型
 */
public enum TradeTypeEnum {

    SEND_GIFT_TRADE(0,"送礼物交易"),
    LIVING_RECHARGE(1,"直播间充值"),
    GET_RED_PACKET(2, "抢红包得到的"),
    PAY_ORDER(3, "支付订单");

    int code;
    String desc;

    /**
     * 判断用户传入的这个code值是否正确
     * @param code
     * @return
     */

    public static boolean isContain(int code) {
        TradeTypeEnum[] values = TradeTypeEnum.values();
        for (TradeTypeEnum value : values) {
            if (value.getCode() == code) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据code获取枚举对象
     * @param code
     * @return
     */
    public static TradeTypeEnum getByCode(int code) {
        TradeTypeEnum[] values = TradeTypeEnum.values();
        for (TradeTypeEnum value : values) {
            if (value.getCode() == code) {
                return value;
            }
        }
        return null;
    }

    TradeTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
