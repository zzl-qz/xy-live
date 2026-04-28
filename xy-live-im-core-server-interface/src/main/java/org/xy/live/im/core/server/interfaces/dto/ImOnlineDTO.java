package org.xy.live.im.core.server.interfaces.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户上线DTO
 */
@Data
public class ImOnlineDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3106376010560727999L;
    private Long userId;
    private Integer appId;
    private Integer roomId;
    private long loginTime;
}
