package org.xy.live.user.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.user.dto.UserDTO;
import org.xy.live.user.interfaces.rpc.IUserRpc;
import org.xy.live.user.provider.service.IUserService;

import java.util.List;
import java.util.Map;

@DubboService
public class UserRpcImpl implements IUserRpc {


    @Resource
    private IUserService userService;

    @Override
    public UserDTO getByUserId(Long userId) {
        return userService.getByUserId(userId);
    }

    @Override
    public boolean updateUserInfo(UserDTO userDTO) {
        return userService.updateUserInfo(userDTO);
    }

    @Override
    public boolean insertOne(UserDTO userDTO) {
        return userService.insertOne(userDTO);
    }

    @Override
    public Map<Long, UserDTO> batchQueryUserInfo(List<Long> userIdList) {
        System.out.println("batchQueryUserInfo  " + userIdList.size());
        return userService.batchQueryUserInfo(userIdList);
    }
}
