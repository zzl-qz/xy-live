package org.xy.live.im.core.server.interfaces.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户下线DTO
 */
@Data
public class ImOfflineDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 5005291896846703608L;
    private Long userId;
    private Integer appId;
    private Integer roomId;
    private long logoutTime;
}