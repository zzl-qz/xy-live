package org.xy.live.user.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.user.dto.UserLoginDTO;
import org.xy.live.user.dto.UserPhoneDTO;
import org.xy.live.user.interfaces.rpc.IUserPhoneRpc;
import org.xy.live.user.provider.service.IUserPhoneService;

import java.util.List;

@DubboService
public class IUserPhoneRpcImpl implements IUserPhoneRpc {

    @Resource
    private IUserPhoneService userPhoneService;

    @Override
    public UserLoginDTO login(String phone) {
        System.out.println("测试" + phone);
        return userPhoneService.login(phone);
    }

    @Override
    public UserPhoneDTO queryByPhone(String phone) {
        return userPhoneService.queryByPhone(phone);
    }

    @Override
    public List<UserPhoneDTO> queryByUserId(Long userId) {
        return userPhoneService.queryByUserId(userId);
    }
}
