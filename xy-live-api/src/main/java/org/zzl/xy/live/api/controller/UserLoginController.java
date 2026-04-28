package org.zzl.xy.live.api.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xy.live.common.interfaces.vo.WebResponseVO;
import org.zzl.xy.live.api.service.IUserLoginService;
import org.zzl.xy.live.api.vo.UserLoginVO;

@RestController
@RequestMapping("/userLogin")
public class UserLoginController {

    @Resource
    private IUserLoginService userLoginService;

    /**
     * 发送验证码
     * @param phone
     * @return
     */
    @PostMapping("/sendLoginCode")
    public WebResponseVO<Object> sendLoginCode(String phone) {
        return userLoginService.sendLoginCode(phone);
    }

    /**
     * 登录请求 验证码是否合法 -> 初始化注册/老用户登录
      * @param phone
     * @param code
     * @param response
     * @return
     */
    @PostMapping("/login")
    public WebResponseVO<UserLoginVO> login(String phone, Integer code, HttpServletResponse response) {
        return userLoginService.login(phone, code, response);
    }

}