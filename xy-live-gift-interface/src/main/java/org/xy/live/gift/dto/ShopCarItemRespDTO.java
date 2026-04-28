package org.xy.live.gift.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ShopCarItemRespDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 7247175817439564893L;

    private Integer count;          // 购买数量
    private boolean selected;       // 是否勾选（新增）
    private Integer roomId;         // 商品来源直播间
    private SkuInfoDTO skuInfoDTO;  // 商品详情
}