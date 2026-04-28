package org.zzl.xy.live.api.controller;


import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;
import org.xy.live.user.dto.UserDTO;
import org.xy.live.user.interfaces.rpc.IUserRpc;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/user")
@Deprecated
public class UserController {

    @DubboReference
    private IUserRpc userRpc;

    /**
     * 获取用户信息
     * @param userId
     * @return
     */
    @GetMapping("/getUserInfo")
    public UserDTO getUserInfo(@RequestParam("userId") Long userId){
       return userRpc.getByUserId(userId);
    }

    /**
     * 修改用户数据
     * @param userId
     * @param nickName
     * @return
     */
    @GetMapping("/updateUserInfo")
    public boolean updateUserInfo(@RequestParam("userId") Long userId, @RequestParam("nickName") String nickName) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setNickName(nickName);
        return userRpc.updateUserInfo(userDTO);
    }

    /**
     * 新增一条数据
     * @param userId
     * @return
     */
    @GetMapping("/insertOne")
    public boolean insertOne(@RequestParam("userId") Long userId) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setNickName("zzl");
        userDTO.setSex(1);
        return userRpc.insertOne(userDTO);
    }

    /**
     * 批量查询用户信息
     * @param userIdStr
     * @return
     */
    @GetMapping("/batchQueryUserInfo")
    public Map<Long, UserDTO> batchQueryUserInfo(String userIdStr) {
        return userRpc.batchQueryUserInfo(Arrays.asList(userIdStr.split(","))
                .stream().map(Long::valueOf).collect(Collectors.toList()));
    }

}
