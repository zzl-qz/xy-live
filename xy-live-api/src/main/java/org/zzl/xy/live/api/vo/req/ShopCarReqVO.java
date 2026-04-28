package org.zzl.xy.live.api.vo.req;

import lombok.Data;
import org.zzl.xy.live.api.annotations.EncryptId;

@Data
public class ShopCarReqVO {


    private Long skuId; // 商品Id


    private Integer roomId; // 直播间Id
}