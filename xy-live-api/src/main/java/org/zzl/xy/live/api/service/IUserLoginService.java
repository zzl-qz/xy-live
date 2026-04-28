package org.zzl.xy.live.api.service;

import jakarta.servlet.http.HttpServletResponse;
import org.xy.live.common.interfaces.vo.WebResponseVO;
import org.zzl.xy.live.api.vo.UserLoginVO;

public interface IUserLoginService {

    /**
     * 发送登录验证码
     *
     * @param phone
     * @return
     */
    WebResponseVO<Object> sendLoginCode(String phone);

    /**
     * 手机号+验证码登录
     *
     * @param phone
     * @param code
     * @return
     */
    WebResponseVO<UserLoginVO> login(String phone, Integer code, HttpServletResponse response);
}