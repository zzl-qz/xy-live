package org.xy.live.im.core.server.interfaces.enums;

public enum ParamCodeEnum {
    LIVING_ROOM_LOGIN(1001, "直播间登录");
    int code;
    String desc;
    ParamCodeEnum(int code, String desc) {
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
