package org.xy.live.bank.provider.service;

import org.xy.live.bank.interfaces.dto.PayProductDTO;

import java.util.List;

public interface PayProductService {
    /**
     * 根据产品类型，返回批量的商品信息
     * @param type
     * @return
     */
    List<PayProductDTO> products(Integer type);

    /**
     * 根据产品Id查询商品信息
     * @param productId
     * @return
     */
    PayProductDTO getByProductId(Integer productId);
}
