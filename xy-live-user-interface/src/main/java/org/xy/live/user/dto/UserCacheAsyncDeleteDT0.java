package org.xy.live.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 延迟消息类
 */
@Data
public class UserCacheAsyncDeleteDT0 implements Serializable {

    @Serial
    private static final long serialVersionUID = 9144025905355048277L;


    /**
     * 区分不用业务的code
     */
    private int code;

    private String json;

}
