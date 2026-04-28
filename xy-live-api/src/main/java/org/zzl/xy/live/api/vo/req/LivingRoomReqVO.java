package org.zzl.xy.live.api.vo.req;

import lombok.Data;
import org.zzl.xy.live.api.annotations.EncryptId;

@Data
public class LivingRoomReqVO {
    private Integer type;
    private int page;
    private int pageSize;


    private Integer roomId; // 房间号

    private String redPacketConfigCode; // 红包雨唯一标识（如果有的话）
}