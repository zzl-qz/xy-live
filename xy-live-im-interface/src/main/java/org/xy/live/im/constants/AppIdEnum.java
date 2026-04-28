package org.xy.live.im.constants;

public enum AppIdEnum {
    XY_LIVE_BIZ(10001, "星跃直播业务");

    private int code;
    private String desc;

    AppIdEnum(int code, String desc) {
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