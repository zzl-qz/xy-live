package org.zzl.xy.live.api.vo.resp;

import lombok.Data;
import org.zzl.xy.live.api.annotations.DecryptId;

/**
 * 单个产品
 */
@Data
public class PayProductItemVO {


    private Long id;
    private String name;
    private Integer coinNum;
    
}