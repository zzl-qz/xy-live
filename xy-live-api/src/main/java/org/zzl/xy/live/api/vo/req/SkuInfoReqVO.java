package org.zzl.xy.live.api.vo.req;

import lombok.Data;
import org.zzl.xy.live.api.annotations.EncryptId;

@Data
public class SkuInfoReqVO {

    private Long skuId;

    private Long anchorId;
}