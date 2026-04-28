package org.zzl.xy.live.api.vo.resp;

import lombok.Data;
import org.zzl.xy.live.api.annotations.DecryptId;

@Data
public class PayProductRespVO {

    // 响应给前端的订单Id

    private String orderId;
    
}