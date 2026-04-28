package org.xy.live.bank.interfaces.constants;


/**
 * 支付渠道类型，就是标记这笔订单是从哪里发起的，可能是用户再直播间中充值，也可能是在用户中心（个人中心）
 */
public enum PaySourceEnum {

    XY_LIVING_ROOM(1,"星跃直播间内支付"),
    XY_USER_CENTER(2,"用户中心");

    PaySourceEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 找到支付渠道来源
     * @param code
     * @return
     */
    public static PaySourceEnum find(int code) {
        for (PaySourceEnum value : PaySourceEnum.values()) {
            if(value.getCode() == code) {
                return value;
            }
        }
        return null;
    }

    private int code;
    private String desc;

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
