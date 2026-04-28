package org.zzl.xy.live.api.annotations;

import java.lang.annotation.*;

/**
 * 标记某个字段（通常是 ID 字段）在返回给前端时需要自动加密
 * 一般用于 Long 类型的 id 字段
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EncryptId {
}