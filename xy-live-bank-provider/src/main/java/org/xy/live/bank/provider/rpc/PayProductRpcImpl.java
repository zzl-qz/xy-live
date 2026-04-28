package org.xy.live.bank.provider.rpc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.bank.interfaces.dto.PayProductDTO;
import org.xy.live.bank.interfaces.rpc.PayProductRpc;
import org.xy.live.bank.provider.dao.mapper.PayProductMapper;
import org.xy.live.bank.provider.dao.po.PayProductPO;
import org.xy.live.bank.provider.service.PayProductService;
import org.xy.live.common.interfaces.enums.CommonStatusEnum;

import java.util.List;

@DubboService
public class PayProductRpcImpl implements PayProductRpc {

    @Resource
    private PayProductService payProductService;

    @Override
    public List<PayProductDTO> products(Integer type) {
        return payProductService.products(type);
    }

    @Override
    public PayProductDTO getByProductId(Integer productId) {
        return payProductService.getByProductId(productId);
    }
}
