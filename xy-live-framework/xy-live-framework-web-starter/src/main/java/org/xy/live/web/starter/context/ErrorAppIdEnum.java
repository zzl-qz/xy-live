package org.xy.live.web.starter.context;

public enum ErrorAppIdEnum {

    XY_API_ERROR(101,"xy-live-api");

    ErrorAppIdEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    int code;
    String msg;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}