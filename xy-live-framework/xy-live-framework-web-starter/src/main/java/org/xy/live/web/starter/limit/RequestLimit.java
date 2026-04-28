package org.xy.live.web.starter.limit;


import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestLimit {

    /**
     * 允许请求的量
     * @return
     */
    int limit();

    /**
     * 限流的时长
     * @return
     */
    int second();

    /**
     * 提示内容
     * @return
     */
    String msg() default "请求过于频繁";

}
