package org.xy.live.gift.rpc;

import org.xy.live.gift.dto.SkuDetailInfoDTO;
import org.xy.live.gift.dto.SkuInfoDTO;

import java.util.List;

public interface ISkuInfoRpc {

    /**
     * 根据主播Id查询商品信息
     */
    List<SkuInfoDTO> queryByAnchorId(Long anchorId);

    /**
     * 根据skuId 查询Sku信息
     */
    SkuDetailInfoDTO queryBySkuId(Long skuId, Long anchorId);
}   