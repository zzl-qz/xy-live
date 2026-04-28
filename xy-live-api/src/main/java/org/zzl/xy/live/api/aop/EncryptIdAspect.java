package org.zzl.xy.live.api.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.xy.live.common.interfaces.utils.AESUtils;
import org.zzl.xy.live.api.annotations.EncryptId;
import org.zzl.xy.live.api.constants.SALTConstant;

@Aspect
@Component
public class EncryptIdAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptIdAspect.class);


    /**
     * 拦截所有 Controller 方法的返回值
     */
    @Around("execution(* org.zzl.xy.live.api.controller..*.*(..))")
    public Object encryptIdFieldsInResponse(ProceedingJoinPoint joinPoint) throws Throwable {

        LOGGER.info("执行到加密方法了");

        // 1. 先执行 Controller 方法，获取原始返回值
        Object returnValue = joinPoint.proceed();

        if (returnValue == null) {
            return null;
        }

        // 2. 自动查找对象中带有 @EncryptId 注解的字段，并加密其值
        encryptAnnotatedFields(returnValue);

        // 3. 返回处理后的对象（某些字段已经被替换为加密字符串）
        return returnValue;
    }

    /**
     * 递归地查找对象中的字段，并对标记了 @EncryptId 的 Long 类型字段进行加密
     */
    private void encryptAnnotatedFields(Object obj) {
        if (obj == null) {
            return;
        }

        Class<?> clazz = obj.getClass();
        // 只处理非 Java 原生类（避免干扰框架类）
        if (clazz.isPrimitive() || isJavaClass(clazz)) {
            return;
        }

        // 遍历所有字段
        ReflectionUtils.doWithFields(clazz, field -> {
            // 设置可访问私有字段
            field.setAccessible(true);

            // 只处理标记了 @EncryptId 的字段
            if (field.isAnnotationPresent(EncryptId.class)) {
                Object fieldValue = field.get(obj);

                // 只处理 Long 类型（或者 Long 包装类）
                if (fieldValue instanceof Long) {
                    String idValue = fieldValue.toString();
                    // 加密该 ID
                    String encryptedValue = null;
                    try {
                        // 进行加密
                        encryptedValue = AESUtils.encrypt(idValue, SALTConstant.SAFE_SALT);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    // 将字段值替换为加密后的字符串
                    field.set(obj, encryptedValue);
                }
                // 可扩展支持其他类型，如 String id、自定义ID类型等
            }
        }, field -> !field.isSynthetic()); // 排除编译器生成的合成字段
    }

    /**
     * 判断是否为 JDK 自带类（避免处理框架内部类）
     */
    private boolean isJavaClass(Class<?> clazz) {
        return clazz != null && clazz.getClassLoader() == null;
    }
}