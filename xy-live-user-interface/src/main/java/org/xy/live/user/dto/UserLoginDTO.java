package org.xy.live.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserLoginDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -4290788036479984698L;
    
    private Long userId;
    private String token;
    private boolean loginStatus;
    String desc;

    /**
     * 获取登录失败DTO
     *
     * @param desc
     * @return
     */
    public static UserLoginDTO loginError(String desc) {
        UserLoginDTO userLoginDTO = new UserLoginDTO();
        userLoginDTO.setLoginStatus(false);
        userLoginDTO.setDesc(desc);
        return userLoginDTO;
    }


    /**
     * 获取登录成功DTO
     *
     * @param userId
     * @param token
     * @return
     */
    public static UserLoginDTO loginSuccess(Long userId, String token) {
        UserLoginDTO userLoginDTO = new UserLoginDTO();
        userLoginDTO.setLoginStatus(true);
        userLoginDTO.setUserId(userId);
        userLoginDTO.setToken(token);
        return userLoginDTO;
    }
    
}