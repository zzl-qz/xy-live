package org.xy.live.bank.interfaces.rpc;

import org.xy.live.bank.interfaces.dto.PayProductDTO;

import java.util.List;

public interface PayProductRpc {

    /**
     * 根据产品类型，返回批量的商品信息
     */
    List<PayProductDTO> products(Integer type);

    /**
     * 根据产品id查询
     */
    PayProductDTO getByProductId(Integer productId);
}