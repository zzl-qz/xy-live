package org.zzl.xy.live.api.vo;

import lombok.Data;
import org.zzl.xy.live.api.annotations.DecryptId;

@Data
public class HomePageVO {

    private boolean loginStatus;

    private long userId;
    private String nickName;
    private String avatar;
    //是否是主播身份
    private boolean showStartLivingBtn;
}