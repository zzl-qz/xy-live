package org.xy.live.web.starter.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xy.live.common.interfaces.vo.WebResponseVO;

/**
 * 全局异常处理器，Spring的异常处理机制会优先匹配最具体的异常类型
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 捕获全部的异常
     * @param request
     * @param e
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public WebResponseVO errorHandler(HttpServletRequest request, Exception e) {
        LOGGER.error(request.getRequestURI() + ",error is ", e);
        return WebResponseVO.sysError("系统异常");
    }

    /**
     * 捕获XyErrorException
     * @param request
     * @param e
     * @return
     */
    @ExceptionHandler(value = XyErrorException.class)
    @ResponseBody
    public WebResponseVO sysErrorHandler(HttpServletRequest request, XyErrorException e) {
        //业务异常，参数传递有误,都会走到这里
        LOGGER.error(request.getRequestURI() + ",error code is {},error msg is {}", e.getErrorCode(), e.getErrorMsg());
        return WebResponseVO.bizError(e.getErrorCode(), e.getErrorMsg());
    }
}