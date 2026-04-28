package org.zzl.xy.live.api.vo;

import lombok.Data;
import org.zzl.xy.live.api.annotations.DecryptId;
import org.zzl.xy.live.api.annotations.EncryptId;

@Data
public class UserLoginVO {

    private Long userId;
    private String token;
}