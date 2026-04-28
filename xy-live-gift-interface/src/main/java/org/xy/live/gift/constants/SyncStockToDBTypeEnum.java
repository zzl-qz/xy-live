package org.xy.live.gift.constants;

/**
 * @Author: Daylight
 * @Date: 2026-01-27 15:02
 */
public enum SyncStockToDBTypeEnum {
    INCR(0, "增加库存"),
    DECR(1, "减少库存");

    int code;
    String desc;

    SyncStockToDBTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     *  判断是否是有效的值
     * @param code
     * @return
     */
    public static boolean isValid(int code) {
        for (SyncStockToDBTypeEnum value : SyncStockToDBTypeEnum.values()) {
            if (value.code == code) return true;
        }
        return false;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
