package org.zzl.xy.live.api.annotations;

import java.lang.annotation.*;

/**
 * 标记某个参数是加密的 ID，需要在方法执行前进行解密，还原为真实 Long 类型 ID
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DecryptId {
}