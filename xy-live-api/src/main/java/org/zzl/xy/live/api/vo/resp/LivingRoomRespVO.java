package org.zzl.xy.live.api.vo.resp;

import lombok.Data;
import org.zzl.xy.live.api.annotations.DecryptId;

@Data
public class LivingRoomRespVO {

    private Integer id;
    private String roomName;

    private Long anchorId;
    private Integer watchNum;
    private Integer goodNum;
    private String covertImg;
}