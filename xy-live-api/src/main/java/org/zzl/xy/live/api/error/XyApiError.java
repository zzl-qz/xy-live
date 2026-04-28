package org.zzl.xy.live.api.error;


import org.xy.live.web.starter.error.XyBaseError;

public enum XyApiError implements XyBaseError {
    
    LIVING_ROOM_TYPE_MISSING(10001, "需要给定直播间类型"),
    PHONE_NOT_BLANK(10002, "手机号不能为空"),
    PHONE_IN_VALID(10003, "手机号格式异常"),
    LOGIN_CODE_IN_VALID(10004, "验证码格式异常"),
    GIFT_CONFIG_ERROR(10005, "礼物配置异常"),
    NOT_SEND_TO_YOURSELF(10006, "不能给自己送礼"),
    SEND_GIFT_ERROR(10007, "送礼失败"),
    PK_ONLINE_BUSY(10008, "当前已有人正在pk连线中，请稍后再试"),
    NO_ANCHOR(10009, "你不是主播，无权开播"),
    NO_USER_EXIT(10010, "无相应用户信息存在，请登录后再操作"),
    ;


    XyApiError(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    int code;
    String desc;


    @Override
    public int getErrorCode() {
        return code;
    }

    @Override
    public String getErrorMsg() {
        return desc;
    }
}