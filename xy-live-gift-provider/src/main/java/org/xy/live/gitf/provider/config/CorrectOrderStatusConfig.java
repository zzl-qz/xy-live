package org.xy.live.gitf.provider.config;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.xy.live.bank.interfaces.constants.OrderStatusEnum;
import org.xy.live.bank.interfaces.dto.XyCurrencyTradeDTO;
import org.xy.live.bank.interfaces.rpc.XyCurrencyTradeRpc;
import org.xy.live.common.interfaces.enums.TradeTypeEnum;
import org.xy.live.framework.redis.starter.key.GiftProviderCacheKeyBuilder;
import org.xy.live.gift.constants.SkuOrderInfoEnum;
import org.xy.live.gift.dto.SkuOrderInfoReqDTO;
import org.xy.live.gift.rpc.ISkuStockInfoRpc;
import org.xy.live.gitf.provider.service.IAnchorShopInfoService;
import org.xy.live.gitf.provider.service.ISkuOrderInfoService;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
public class CorrectOrderStatusConfig {

    private final static Logger LOGGER = LoggerFactory.getLogger(CorrectOrderStatusConfig.class);

    @Resource
    private ISkuOrderInfoService skuOrderInfoService;

    @DubboReference
    private XyCurrencyTradeRpc xyCurrencyTradeRpc;

    @Scheduled(cron = "*/50 * * * * ? ")
    public void correctOrderStatus() {
        LOGGER.info("[CorrectOrderStatusConfig] 开始执行定时任务");
        List<SkuOrderInfoReqDTO> skuOrderInfoReqDTOS = skuOrderInfoService.selectByStatus(SkuOrderInfoEnum.PAYING.getCode());
        for (SkuOrderInfoReqDTO order : skuOrderInfoReqDTOS) {
            XyCurrencyTradeDTO orderTrade = xyCurrencyTradeRpc.getByExtraCol(TradeTypeEnum.PAY_ORDER.getCode(), String.valueOf(order.getId()));
            if (orderTrade != null) {
                // 修改订单状态为已支付
                SkuOrderInfoReqDTO reqDTO = new SkuOrderInfoReqDTO();
                reqDTO.setId(order.getId());
                reqDTO.setUserId(order.getUserId());
                reqDTO.setRoomId(order.getRoomId());
                reqDTO.setStatus(SkuOrderInfoEnum.HAS_PAY.getCode());
                LOGGER.info("[correctOrderStatus] 订单已经支付，现在开始更新订单真正的状态，reqDTO is {}", reqDTO);
                skuOrderInfoService.updateOrderStatus(reqDTO, SkuOrderInfoEnum.PAYING.getCode());
            }
        }
    }
}