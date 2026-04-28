package org.zzl.xy.live.api.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xy.live.common.interfaces.utils.AESUtils;
import org.zzl.xy.live.api.annotations.DecryptId;
import org.zzl.xy.live.api.constants.SALTConstant;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
public class DecryptIdAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecryptIdAspect.class);


    /**
     * 拦截所有 Controller 方法
     */
    @Around("execution(* org.zzl.xy.live.api.controller..*.*(..))")
    public Object decryptIdParameters(ProceedingJoinPoint joinPoint) throws Throwable {
        LOGGER.info("执行到解密方法了");
        // 1. 获取方法参数值数组
        Object[] args = joinPoint.getArgs();

        // 2. 获取方法签名与参数信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();

        // 3. 遍历所有参数
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Object arg = args[i];

            // 4. 判断该参数是否标注了 @DecryptId，且类型为 String（加密传来的 ID）
            if (param.isAnnotationPresent(DecryptId.class) && arg instanceof String) {
                String encryptedId = (String) arg;

                try {
                    // 5. 调用 AES 解密，得到真实的 ID 字符串，然后转为 Long
                    String decryptedValue = AESUtils.decrypt(encryptedId, SALTConstant.SAFE_SALT);
                    Long realId = Long.parseLong(decryptedValue);

                    // 6. 替换参数值
                    args[i] = realId; // 直接替换参数数组中的值
                } catch (Exception e) {
                    throw new IllegalArgumentException("解密 ID 失败，参数值: " + encryptedId, e);
                }
            }
        }

        // 7. 使用替换后的参数数组，继续执行原方法
        return joinPoint.proceed(args);
    }
}