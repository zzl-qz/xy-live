package org.xy.live.common.interfaces.vo;

import lombok.Data;

/**
 * 统一返回给前端的VO对象
 */
@Data
public class WebResponseVO<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> WebResponseVO<T> bizError(String msg) {
        WebResponseVO<T> webResponseVO = new WebResponseVO<T>();
        webResponseVO.setCode(501);
        webResponseVO.setMsg(msg);
        return webResponseVO;
    }

    public static <T> WebResponseVO<T> bizError(int code, String msg) {
        WebResponseVO<T> webResponseVO = new WebResponseVO<T>();
        webResponseVO.setCode(code);
        webResponseVO.setMsg(msg);
        return webResponseVO;
    }


    public static <T> WebResponseVO<T> sysError() {
        WebResponseVO<T> webResponseVO = new WebResponseVO<T>();
        webResponseVO.setCode(500);
        return webResponseVO;
    }

    public static <T> WebResponseVO<T> sysError(String msg) {
        WebResponseVO<T> webResponseVO = new WebResponseVO<T>();
        webResponseVO.setCode(500);
        webResponseVO.setMsg(msg);
        return webResponseVO;
    }

    public static <T> WebResponseVO<T> errorParam() {
        WebResponseVO<T> webResponseVO = new WebResponseVO<T>();
        webResponseVO.setCode(400);
        webResponseVO.setMsg("error-param");
        return webResponseVO;
    }

    public static <T> WebResponseVO<T> errorParam(String msg) {
        WebResponseVO<T> webResponseVO = new WebResponseVO<T>();
        webResponseVO.setCode(400);
        webResponseVO.setMsg(msg);
        return webResponseVO;
    }

    public static <T> WebResponseVO<T> success() {
        WebResponseVO<T> webResponseVO = new WebResponseVO<T>();
        webResponseVO.setCode(200);
        webResponseVO.setMsg("success");
        return webResponseVO;
    }

    public static <T> WebResponseVO<T> success(T data) {
        WebResponseVO<T> webResponseVO = new WebResponseVO<T>();
        webResponseVO.setData(data);
        webResponseVO.setCode(200);
        webResponseVO.setMsg("success");
        return webResponseVO;
    }
}