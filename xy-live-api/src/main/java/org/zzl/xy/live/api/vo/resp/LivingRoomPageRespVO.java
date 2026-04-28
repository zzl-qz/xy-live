package org.zzl.xy.live.api.vo.resp;

import lombok.Data;
import org.zzl.xy.live.api.vo.resp.LivingRoomRespVO;

import java.util.List;

@Data
public class LivingRoomPageRespVO {
    
    private List<LivingRoomRespVO> list;
    private boolean hasNext;
}