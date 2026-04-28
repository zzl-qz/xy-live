package org.xy.live.bank.interfaces.constants;

/**
 * 产品类型
 */
public enum PayProductTypeEnum {

    XY_COIN(0,"直播间充值-星跃虚拟币产品");

    PayProductTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    Integer code;
    String desc;

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
