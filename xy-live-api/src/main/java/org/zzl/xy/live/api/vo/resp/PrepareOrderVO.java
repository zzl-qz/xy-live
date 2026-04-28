package org.zzl.xy.live.api.vo.resp;

import lombok.Data;
import org.zzl.xy.live.api.annotations.DecryptId;

@Data
public class PrepareOrderVO {


    private Long userId;

    private Integer roomId;
}