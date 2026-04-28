package org.xy.live.gitf.provider.rpc;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.xy.live.bank.interfaces.rpc.XyCurrencyAccountRpc;
import org.xy.live.common.interfaces.enums.TradeTypeEnum;
import org.xy.live.common.interfaces.topic.GiftProviderTopicNames;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.framework.redis.starter.key.GiftProviderCacheKeyBuilder;
import org.xy.live.gift.constants.SkuOrderInfoEnum;
import org.xy.live.gift.dto.*;
import org.xy.live.gift.rpc.ISkuOrderInfoRpc;
import org.xy.live.gitf.provider.bo.PaymentSuccessHandleBO;
import org.xy.live.gitf.provider.bo.RollBackStockBO;
import org.xy.live.gitf.provider.dao.po.SkuInfoPO;
import org.xy.live.gitf.provider.dao.po.SkuOrderInfoPO;
import org.xy.live.gitf.provider.service.IShopCarService;
import org.xy.live.gitf.provider.service.ISkuInfoService;
import org.xy.live.gitf.provider.service.ISkuOrderInfoService;
import org.xy.live.gitf.provider.service.ISkuStockInfoService;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DubboService
public class SkuOrderInfoRpcImpl implements ISkuOrderInfoRpc {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkuOrderInfoRpcImpl.class);
    
    @Resource
    private ISkuOrderInfoService skuOrderInfoService;
    
    @Resource
    private IShopCarService shopCarService;

    @Resource
    private ISkuStockInfoService skuStockInfoService;

    @Resource
    private MQProducer mqProducer;

    @Resource
    private ISkuInfoService skuInfoService;


    @Resource
    private XyCurrencyAccountRpc xyCurrencyAccountRpc;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private GiftProviderCacheKeyBuilder cacheKeyBuilder;

    @Override
    public List<SkuOrderInfoRespDTO> queryByUserIdAndRoomId(Long userId, Integer roomId) {
        return skuOrderInfoService.queryByUserIdAndRoomId(userId, roomId);
    }

    @Override
    public List<SkuOrderInfoRespDTO> queryByUserId(Long userId) {
        return skuOrderInfoService.queryByUserId(userId);
    }

    @Override
    public boolean insertOne(SkuOrderInfoReqDTO skuOrderInfoReqDTO) {
        return skuOrderInfoService.insertOne(skuOrderInfoReqDTO) != null;
    }

    @Override
    public boolean updateOrderStatus(SkuOrderInfoReqDTO skuOrderInfoReqDTO, Integer oldTypeCode) {
        return skuOrderInfoService.updateOrderStatus(skuOrderInfoReqDTO, oldTypeCode);
    }

    @Override
    public SkuPrepareOrderInfoDTO prepareOrder(PrepareOrderReqDTO reqDTO) {
        // 获取锁失败
        String lockKey = cacheKeyBuilder.buildPrepareOrderLock(reqDTO.getUserId(), reqDTO.getRoomId());
        Boolean getLockSuccess = redisTemplate.opsForValue().setIfAbsent(lockKey, "lockKey", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(getLockSuccess)) {
            return new SkuPrepareOrderInfoDTO();
        }

        // 获取锁成功
        try {
            LOGGER.info("[prepareOrder]  开始执行预下单操作，reqDTO is {}", reqDTO);
            // 查询出购物车信息
            ShopCarReqDTO shopCarReqDTO = ConvertBeanUtils.convert(reqDTO, ShopCarReqDTO.class);
            ShopCarRespDTO carInfo = shopCarService.getCarInfo_v2(shopCarReqDTO);
            LOGGER.info("[prepareOrder]  拿到的购物车信息，carInfo is {}", carInfo);
            List<ShopCarItemRespDTO> carItemList = carInfo.getSkuCarItemRespDTOS();
            if (CollectionUtils.isEmpty(carItemList)) {
                return new SkuPrepareOrderInfoDTO();
            }

            // 用于存储库存扣减成功的商品
            List<ShopCarItemRespDTO> successfulItems = new ArrayList<>();
            // 预扣减库存
            for (ShopCarItemRespDTO shopCarItemRespDTO : carItemList) {
                Long skuId = shopCarItemRespDTO.getSkuInfoDTO().getSkuId();
                Integer count = shopCarItemRespDTO.getCount();
                boolean isSuccess = skuStockInfoService.decrStockNumBySkuIdByLua(skuId, count);
                if (isSuccess) {
                    LOGGER.info("[prepareOrder]  商品 {} 库存扣减成功, count is {}", skuId, count);
                    successfulItems.add(shopCarItemRespDTO);
                } else {
                    // 扣减失败，记录日志并跳过该商品
                    LOGGER.warn("[prepareOrder]  商品 {} 库存不足，请求扣减数量：{}，已从订单中移除", skuId, count);
                }
            }

            // 如果所有商品都扣减失败，直接返回空订单
            if (CollectionUtils.isEmpty(successfulItems)) {
                LOGGER.warn("用户 {} 的订单所有商品库存扣减失败", reqDTO.getUserId());
                return new SkuPrepareOrderInfoDTO();
            }

            // 构建 skuId:count 格式的字符串列表（含数量）
            List<String> skuIdCountList = successfulItems.stream()
                    .map(item -> item.getSkuInfoDTO().getSkuId() + ":" + item.getCount())
                    .collect(Collectors.toList());
            LOGGER.info("[prepareOrder]  successfulItems is {}", successfulItems);

            // 提取纯SKU ID用于后续过滤（无需重复扣库存）
            List<Long> skuIdList = successfulItems.stream()
                    .map(item -> item.getSkuInfoDTO().getSkuId())
                    .toList();
            LOGGER.info("[prepareOrder]  skuIdList is {}", skuIdList);


            // 插入一条订单记录到db中
            SkuOrderInfoPO skuOrderInfoPO = skuOrderInfoService.insertOne(new SkuOrderInfoReqDTO(
                    null, reqDTO.getUserId(), reqDTO.getRoomId(), SkuOrderInfoEnum.PREPARE_PAY.getCode(), skuIdCountList));

            // 清空购物车
            shopCarService.clearShopCar_v2(shopCarReqDTO);
            LOGGER.info("[prepareOrder]  清空购物车完成");

            // 发送库存回滚延迟消息
            stockRollBackHandler(reqDTO.getUserId(), skuOrderInfoPO.getId());
            LOGGER.info("[prepareOrder]  发送库存回滚延迟消息");

            // 封装返回对象
            SkuPrepareOrderInfoDTO respDTO = new SkuPrepareOrderInfoDTO();
            List<ShopCarItemRespDTO> itemList = carItemList.stream()
                    .filter(item -> skuIdList.contains(item.getSkuInfoDTO().getSkuId()))
                    .collect(Collectors.toList());
            LOGGER.info("[prepareOrder]  itemList is {}", itemList);

            // 总价计算：单价 × 数量
            respDTO.setTotalPrice(itemList.stream()
                    .map(item -> item.getSkuInfoDTO().getSkuPrice() * item.getCount())
                    .reduce(Integer::sum)
                    .orElse(0));

            respDTO.setSkuPrepareOrderItemInfoDTOS(itemList);
            LOGGER.info("[prepareOrder]  respDTO is {}", respDTO);
            return respDTO;
        } catch (Exception e) {
            LOGGER.error("[prepareOrder]  errMsg is {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public boolean payNow(Long userId, Integer roomId) {
        // 先查询出订单信息
        List<SkuOrderInfoRespDTO> skuOrderInfoRespDTOS = skuOrderInfoService.queryByUserIdAndRoomId(userId, roomId);
        List<SkuOrderInfoRespDTO> skuOrderInfoRespDTOList = skuOrderInfoRespDTOS.stream().filter(s -> s.getStatus().equals(SkuOrderInfoEnum.PREPARE_PAY.getCode())).toList();
        for (SkuOrderInfoRespDTO order : skuOrderInfoRespDTOList) {
            // 加分布式锁
            String lockKey = cacheKeyBuilder.buildPayOrderLock(userId, order.getId());
            Boolean getLockSuccess = redisTemplate.opsForValue().setIfAbsent(lockKey, "payOrderLock", 5, TimeUnit.MINUTES);
            if (Boolean.FALSE.equals(getLockSuccess)) {
                LOGGER.warn("[payNow] orderId is {} 获取分布式锁失败，可能是已经有人在进行操作了", order.getId());
                continue;
            }
            // 修改为支付中状态
            if (!updateOrderStatus(order.getId(), SkuOrderInfoEnum.PAYING.getCode(), SkuOrderInfoEnum.PREPARE_PAY.getCode())) {
                LOGGER.warn("[payNow] orderId is {} 修改为支付中状态失败", order.getId());
                redisTemplate.delete(lockKey);
                continue;
            }
            // 扣减余额并修改订单状态为已支付
            boolean success = this.processSingleOrderPayment(order);
            if (!success) {
                // todo 这里后续可以考虑接入一些短信通知的功能，告知给用户具体的失败原因
                // 记录下失败的订单Id
                LOGGER.warn("[payNow] orderId is {} 支付失败", order.getId());
            }
            redisTemplate.delete(lockKey);
        }
        return true;
    }


    @Override
    public boolean payNowByOrderId(Long orderId) {
        // todo 这里如果想要实现避免重复支付还需要上层透传一个userId过来
        // 查询出订单
        SkuOrderInfoRespDTO skuOrderInfoRespDTO = skuOrderInfoService.queryByOrderId(orderId);
        // 判断订单状态
        if (!skuOrderInfoRespDTO.getStatus().equals(SkuOrderInfoEnum.PREPARE_PAY.getCode())) {
            return false;
        }
        // 修改为支付中状态
        if (!updateOrderStatus(orderId, SkuOrderInfoEnum.PAYING.getCode(), SkuOrderInfoEnum.PREPARE_PAY.getCode())) {
            LOGGER.warn("[payNow] orderId is {} 修改为支付中状态失败", orderId);
            return false;
        }
        return this.processSingleOrderPayment(skuOrderInfoRespDTO);
    }


    /**
     * 执行订单支付逻辑(公共部分)
     * @param order
     * @return
     */
    private boolean processSingleOrderPayment(SkuOrderInfoRespDTO order) {
        try {
            // 1. 解析商品ID和数量列表（格式：skuId:count,skuId:count）
            String skuIdCountList = order.getSkuIdList();
            if (StringUtil.isEmpty(skuIdCountList)) {
                LOGGER.warn("订单 {} 商品列表为空", order.getId());
                return false;
            }

            // 2. 拆分出所有SKU ID用于批量查询商品信息
            List<Long> skuIds = Arrays.stream(skuIdCountList.split(","))
                    .map(s -> s.split(":"))
                    .map(arr -> Long.valueOf(arr[0]))
                    .collect(Collectors.toList());

            // 3. 批量查询商品信息
            List<SkuInfoPO> skuInfos = skuInfoService.queryBySkuIds(skuIds);
            if (CollectionUtils.isEmpty(skuInfos)) {
                LOGGER.warn("订单 {} 商品信息为空，skuIdList: {}", order.getId(), skuIdCountList);
                return false;
            }

            // 4. 转换为Map便于快速查询（skuId -> SkuInfoPO）
            Map<Long, SkuInfoPO> skuInfoMap = skuInfos.stream()
                    .collect(Collectors.toMap(SkuInfoPO::getSkuId, sku -> sku));

            // 5. 计算总价（单价 × 数量）
            int totalPrice = Arrays.stream(skuIdCountList.split(","))
                    .map(s -> s.split(":")) // 拆分出 [skuId, count]
                    .map(arr -> {
                        Long skuId = Long.valueOf(arr[0]);
                        int count = Integer.parseInt(arr[1]);
                        // 从Map中查找商品信息（性能更优）
                        SkuInfoPO sku = skuInfoMap.get(skuId);
                        if (sku == null) {
                            throw new RuntimeException("商品不存在: " + skuId);
                        }
                        return sku.getSkuPrice() * count; // 单价 × 数量
                    })
                    .reduce(0, Integer::sum);


            // 4. 校验用户余额
            Long userId = order.getUserId();
            Integer balance = xyCurrencyAccountRpc.getBalance(userId);
            if (balance == null || balance < totalPrice) {
                LOGGER.warn("用户 {} 余额不足，当前余额：{}，需支付：{}", userId, balance, totalPrice);
                return false;
            }


            // 5. 扣减余额
            boolean decrSuccess = xyCurrencyAccountRpc.decr(userId, totalPrice, TradeTypeEnum.PAY_ORDER.getCode(), String.valueOf(order.getId()));
            if (!decrSuccess) {
                LOGGER.warn("用户 {} 扣减余额失败，金额：{}", userId, totalPrice);
                return false;
            }


            // 6. 更新订单状态为已支付
            SkuOrderInfoReqDTO updateReq = ConvertBeanUtils.convert(order, SkuOrderInfoReqDTO.class);
            updateReq.setStatus(SkuOrderInfoEnum.HAS_PAY.getCode());
            boolean updateSuccess = skuOrderInfoService.updateOrderStatus(updateReq, SkuOrderInfoEnum.PAYING.getCode());

            if (!updateSuccess) {
                LOGGER.error("订单 {} 状态更新失败，回滚余额", order.getId());
                // 回滚余额
                xyCurrencyAccountRpc.incr(userId, totalPrice, TradeTypeEnum.PAY_ORDER.getCode(), String.valueOf(order.getId()));
                return false;
            }
            return true;

        } catch (Exception e) {
            LOGGER.error("处理订单支付异常，订单：{}", order, e);
            return false;
        }
    }



    /**
     * 发送库存回滚延迟消息
     * @param userId
     * @param orderId
     */
    private void stockRollBackHandler(Long userId, Long orderId) {
        // 发送延时MQ：若订单未支付，进行库存回滚
        RollBackStockBO rollBackStockBO = new RollBackStockBO(userId, orderId, 1);
        Message message = new Message();
        message.setTopic(GiftProviderTopicNames.ROLL_BACK_STOCK);
        message.setBody(JSON.toJSONBytes(rollBackStockBO));
        // 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
        message.setDelayTimeLevel(16);
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("发送库存回滚定时消息，[sendResult] is {}", sendResult);
        } catch (Exception e) {
            LOGGER.info("库存回滚定时消息发送失败，错误信息 is", e);
        }
    }

    /**
     * 修改订单状态
     * @param orderId
     * @param status
     * @return
     */
    private boolean updateOrderStatus(Long orderId, Integer status, Integer oldTypeCode) {
        SkuOrderInfoReqDTO reqDTO = new SkuOrderInfoReqDTO();
        reqDTO.setStatus(status);
        reqDTO.setId(orderId);
        return skuOrderInfoService.updateOrderStatus(reqDTO, oldTypeCode);
    }
}