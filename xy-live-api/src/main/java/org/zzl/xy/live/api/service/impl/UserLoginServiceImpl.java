package org.zzl.xy.live.api.service.impl;


import jakarta.servlet.http.HttpServletResponse;
import cn.hutool.core.bean.BeanUtil;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.Cookie;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xy.live.account.interfaces.IAccountTokenRPC;
import org.xy.live.common.interfaces.vo.WebResponseVO;
import org.xy.live.msg.dto.MsgCheckDTO;
import org.xy.live.msg.enums.MsgSendResultEnum;
import org.xy.live.msg.interfaces.ISmsRpc;
import org.xy.live.user.dto.UserLoginDTO;
import org.xy.live.user.interfaces.rpc.IUserPhoneRpc;
import org.xy.live.web.starter.error.ErrorAssert;
import org.zzl.xy.live.api.error.XyApiError;
import org.zzl.xy.live.api.service.IUserLoginService;
import org.zzl.xy.live.api.vo.UserLoginVO;

import java.util.regex.Pattern;

@Service
public class UserLoginServiceImpl implements IUserLoginService {

    private static final String PHONE_REG = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$";
    private static final Logger LOGGER = LoggerFactory.getLogger(UserLoginServiceImpl.class);

    @DubboReference(timeout = 15000)
    private ISmsRpc smsRpc;

    @DubboReference(timeout = 15000)
    private IUserPhoneRpc userPhoneRpc;
    
    @DubboReference(timeout = 15000)
    private IAccountTokenRPC accountTokenRPC;

    /**
     * 发送登录验证码
     *
     * @param phone
     * @return
     */
    @Override
    public WebResponseVO<Object> sendLoginCode(String phone) {
        // 参数校验
        ErrorAssert.isNotBlank(phone, XyApiError.PHONE_NOT_BLANK);
        ErrorAssert.isTure(Pattern.matches(PHONE_REG, phone), XyApiError.PHONE_IN_VALID);
        MsgSendResultEnum msgSendResultEnum = smsRpc.sendLoginCode(phone);
        if (msgSendResultEnum == MsgSendResultEnum.SEND_SUCCESS) {
            return WebResponseVO.success();
        }
        return WebResponseVO.sysError("短信发送太频繁，请稍后再试");
    }

    /**
     * 登录
     *
     * @param phone
     * @param code
     * @param response
     * @return
     */
    @Override
    public WebResponseVO<UserLoginVO> login(String phone, Integer code, HttpServletResponse response) {
        // 参数校验
        ErrorAssert.isNotBlank(phone, XyApiError.PHONE_NOT_BLANK);
        ErrorAssert.isTure(Pattern.matches(PHONE_REG, phone), XyApiError.PHONE_IN_VALID);
        ErrorAssert.isTure(code != null || code >= 100000, XyApiError.LOGIN_CODE_IN_VALID);
        // 检查验证码是否匹配
        MsgCheckDTO msgCheckDTO = smsRpc.checkLoginCode(phone, code);
        if (!msgCheckDTO.isCheckStatus()) {// 校验没通过
            return WebResponseVO.bizError(msgCheckDTO.getDesc());
        }
        // 生成并封装token到cookie返回
        UserLoginDTO userLoginDTO = userPhoneRpc.login(phone);
        String token = accountTokenRPC.createAndSaveLoginToken(userLoginDTO.getUserId());

        UserLoginVO userLoginVO = BeanUtil.copyProperties(userLoginDTO, UserLoginVO.class);
        userLoginVO.setToken(token);
        return WebResponseVO.success(userLoginVO);
    }
}