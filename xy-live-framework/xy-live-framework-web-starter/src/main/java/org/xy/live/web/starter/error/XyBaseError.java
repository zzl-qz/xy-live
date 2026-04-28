package org.xy.live.web.starter.error;

/**
 * 自定义异常的接口规范
 */
public interface XyBaseError {

    int getErrorCode();
    String getErrorMsg();
}