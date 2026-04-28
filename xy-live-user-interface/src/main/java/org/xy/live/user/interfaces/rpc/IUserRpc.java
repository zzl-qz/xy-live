package org.xy.live.user.interfaces.rpc;

import org.xy.live.user.dto.UserDTO;

import java.util.List;
import java.util.Map;

public interface IUserRpc {

    /**
     * 根据用户id获取DTO对象
     * @param userId
     * @return
     */
    UserDTO getByUserId(Long userId);


    /**
     * 修改用户信息
     * @param userDTO
     * @return
     */
    boolean updateUserInfo(UserDTO userDTO);

    /**
     * 新增一条新的数据
     * @param userDTO
     * @return
     */
    boolean insertOne(UserDTO userDTO);

    /**
     * 批量查询用户信息
     * @param userIdList
     * @return
     */
    Map<Long, UserDTO> batchQueryUserInfo(List<Long> userIdList);


}
