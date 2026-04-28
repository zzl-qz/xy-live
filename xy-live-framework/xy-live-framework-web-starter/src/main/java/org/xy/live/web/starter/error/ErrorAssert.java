package org.xy.live.web.starter.error;

/**
 * 自定义断言类
 */
public class ErrorAssert {
    
    /**
     * 判断参数不能为空
     */
    public static void isNotNull(Object obj, XyBaseError xyBaseError) {
        if (obj == null) {
            throw new XyErrorException(xyBaseError);
        }
    }

    /**
     * 判断字符串不能为空
     */
    public static void isNotBlank(String str, XyBaseError xyBaseError) {
        if (str == null || str.trim().length() == 0) {
            throw new XyErrorException(xyBaseError);
        }
    }

    /**
     * flag 为false时执行
     */
    public static void isTure(boolean flag, XyBaseError xyBaseError) {
        if (!flag) {
            throw new XyErrorException(xyBaseError);
        }
    }

}