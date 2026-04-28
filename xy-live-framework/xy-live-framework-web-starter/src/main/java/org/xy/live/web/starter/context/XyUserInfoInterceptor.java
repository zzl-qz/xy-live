package org.xy.live.web.starter.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.xy.live.common.interfaces.enums.GatewayHeaderEnum;

/**
 * 星悦直播 用户信息拦截器
 */
public class XyUserInfoInterceptor implements HandlerInterceptor {

    //所有web请求来到这里的时候，都要被拦截（前置拦截器，controller执行前执行）
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userIdStr = request.getHeader(GatewayHeaderEnum.USER_LOGIN_ID.getName());
        //参数判断，userID是否为空
        //可能走的是白名单url
        if (StringUtils.isEmpty(userIdStr)) {
            System.out.println("白名单请求：放行");
            return true;
        }
        //如果userId不为空，则把它放在线程本地变量里面去
        XyRequestContext.set(RequestConstants.XY_USER_ID, Long.valueOf(userIdStr));
        return true;
    }

    //  Controller 方法执行之后、视图渲染之前被调用,用来清除当前现成的userId信息，避免内存溢出
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        XyRequestContext.clear();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 无论请求成功/失败，最终都会执行这里，兜底清理
        XyRequestContext.clear();
    }

}