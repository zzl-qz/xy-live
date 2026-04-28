package org.xy.live.gitf.provider.service.impl;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.xy.live.common.interfaces.topic.ShopCarProviderTopicNames;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.framework.redis.starter.key.GiftProviderCacheKeyBuilder;
import org.xy.live.gift.constants.SkuNumChangeTypeEnum;
import org.xy.live.gift.dto.*;
import org.xy.live.gitf.provider.bo.CartChangeEventBO;
import org.xy.live.gitf.provider.dao.po.SkuInfoPO;
import org.xy.live.gitf.provider.service.CartItemService;
import org.xy.live.gitf.provider.service.IShopCarService;
import org.xy.live.gitf.provider.service.ISkuInfoService;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ShopCarServiceImpl implements IShopCarService {

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private GiftProviderCacheKeyBuilder cacheKeyBuilder;
    @Resource
    private ISkuInfoService skuInfoService;

    @Resource
    private CartItemService cartItemService;

    @Resource
    private MQProducer mqProducer;

    private static final Logger LOGGER = LoggerFactory.getLogger(ShopCarServiceImpl.class);

    // 购物车最大数量
    private static final int maxSize = 50;

    /**
     * 购物车是以直播间和用户为维度的，所以不需要去进行持久化
     * 如果直播过期了或是关闭了，对应的购物车记录也就删除掉了
     * @param shopCarReqDTO
     * @return
     */
    @Override
    public Boolean addCar(ShopCarReqDTO shopCarReqDTO) {
        Long userId = shopCarReqDTO.getUserId();
        Integer roomId = shopCarReqDTO.getRoomId();
        Long skuId = shopCarReqDTO.getSkuId();
        String cacheKey = cacheKeyBuilder.buildUserShopCar(userId);

        // 如果购物车已经失效了，必需去重新拉取一下最新信息，避免超出
        if (Boolean.FALSE.equals(redisTemplate.hasKey(cacheKey))) {
            this.getCarInfo(shopCarReqDTO);
        }

        // 判断最大数量是否超过
        if (redisTemplate.opsForHash().size(cacheKey) > maxSize) {
            LOGGER.error("超过购物车最大数量");
            return false;
        }

        this.deleteEmptyKey(userId); // 删除空标记
        // 写入购物车
        redisTemplate.opsForHash().put(cacheKey, String.valueOf(skuId),
                JSON.toJSONString(new ShopCarItemDTO(1, true, roomId)));
        // 发送同步消息
        CartChangeEventBO cartChangeEventBO = new CartChangeEventBO();
        cartChangeEventBO.setUserId(userId);
        cartChangeEventBO.setSkuId(skuId);
        cartChangeEventBO.setQty(1);
        cartChangeEventBO.setSelected(true);
        cartChangeEventBO.setRoomId(roomId);
        this.sendSyncMsg(cartChangeEventBO);
        return true;
    }

    /**
     * 从购物车中移除商品
     * @param shopCarReqDTO
     * @return
     */
    @Override
    public Boolean removeFromCar(ShopCarReqDTO shopCarReqDTO) {
        Long userId = shopCarReqDTO.getUserId();
        Long skuId = shopCarReqDTO.getSkuId();
        String cacheKey = cacheKeyBuilder.buildUserShopCar(userId);
        redisTemplate.opsForHash().delete(cacheKey, String.valueOf(skuId));
        // 发送同步消息
        CartChangeEventBO cartChangeEventBO = new CartChangeEventBO();
        cartChangeEventBO.setUserId(userId);
        cartChangeEventBO.setSkuId(skuId);
        this.sendSyncMsg(cartChangeEventBO);
        return true;
    }

    /**
     * 清空购物车
     * @param shopCarReqDTO
     * @return
     */
    @Override
    public Boolean clearShopCar(ShopCarReqDTO shopCarReqDTO) {
        Long userId = shopCarReqDTO.getUserId();
        String cacheKey = cacheKeyBuilder.buildUserShopCar(userId);
        redisTemplate.unlink(cacheKey);
        this.deleteEmptyKey(userId); // 删除空标记
        // 发送清空购物车信息
        CartChangeEventBO cartChangeEventBO = new CartChangeEventBO();
        cartChangeEventBO.setUserId(userId);
        this.sendClearMsg(cartChangeEventBO);
        return true;
    }

    /**
     * 修改商品数量
     * @param shopCarReqDTO
     * @return
     */
    @Override
    public Boolean changeCarItemNum(ShopCarReqDTO shopCarReqDTO, Integer changeType) {
        Long userId = shopCarReqDTO.getUserId();
        Integer roomId = shopCarReqDTO.getRoomId();
        Long skuId = shopCarReqDTO.getSkuId();
        // 判断一下changeType是不是我们提前定义好的类型
        if (!SkuNumChangeTypeEnum.isContain(changeType)) {
            LOGGER.error("changeType 不是规定的范围内的数据, [changeType] is {}", changeType);
            return false;
        }
        String shopCarCacheKey = cacheKeyBuilder.buildUserShopCar(userId);
        Object shopItemObj = redisTemplate.opsForHash().get(shopCarCacheKey, String.valueOf(skuId));
        // 第一次来，直接添加就行了
        if (shopItemObj == null) {
            this.addCar(shopCarReqDTO);
            return true;
        }

        // 否则说明只是需要改变一下数量
        ShopCarItemDTO shopCarItemDTO = JSON.parseObject((String) shopItemObj, ShopCarItemDTO.class);

        // 构建发送同步消息对象
        CartChangeEventBO cartChangeEventBO = new CartChangeEventBO();
        cartChangeEventBO.setUserId(userId);
        cartChangeEventBO.setSkuId(skuId);
        cartChangeEventBO.setSelected(true);
        cartChangeEventBO.setRoomId(roomId);

        // 加1
        if (changeType == SkuNumChangeTypeEnum.ADD_NUM.getCode()) {
            int qty = shopCarItemDTO.getQty() + 1;
            shopCarItemDTO.setQty(qty);
            cartChangeEventBO.setQty(qty);
            redisTemplate.opsForHash().put(shopCarCacheKey, String.valueOf(skuId), JSON.toJSONString(shopCarItemDTO));
        }
        // 减1
        else if (changeType == SkuNumChangeTypeEnum.SUB_NUM.getCode()) {
            Integer qty = shopCarItemDTO.getQty() - 1;
            // 是否需要删除field了
            if (qty <= 0) {
                redisTemplate.opsForHash().delete(shopCarCacheKey, String.valueOf(skuId));
                // 发送删除信息
                this.sendSyncMsg(cartChangeEventBO);
                return true;
            }
            // 塞回去
            shopCarItemDTO.setQty(qty);
            redisTemplate.opsForHash().put(shopCarCacheKey, String.valueOf(skuId), JSON.toJSONString(shopCarItemDTO));
            cartChangeEventBO.setQty(qty); // 设置数量
        }
        // 发送同步消息
        this.sendSyncMsg(cartChangeEventBO);
        return true;
    }

    /**
     * 查询购物车信息
     * @param shopCarReqDTO
     * @return
     */
    @Override
    public ShopCarRespDTO getCarInfo(ShopCarReqDTO shopCarReqDTO) {
        Long userId = shopCarReqDTO.getUserId();
        // 判断是否是空值情况
        String emptyKey = this.buildEmptyKey(userId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(emptyKey))) {
            return new ShopCarRespDTO();
        }

        // 查询缓存
        String cacheKey = cacheKeyBuilder.buildUserShopCar(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cacheKey);
        if (!CollectionUtils.isEmpty(entries)) {
            // k-商品Id   v-数量
            HashMap<Long, ShopCarItemDTO> skuCntMap = new HashMap<>();
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                skuCntMap.put(Long.valueOf((String) entry.getKey()), JSON.parseObject((String) entry.getValue(), ShopCarItemDTO.class));
            }
            // 查询所有的商品
            List<Long> skuIdList = new ArrayList<>(skuCntMap.keySet());
            List<SkuInfoPO> skuInfoPOS = skuInfoService.queryBySkuIds(skuIdList);
            ShopCarRespDTO shopCarRespDTO = new ShopCarRespDTO();
            shopCarRespDTO.setUserId(userId);
            List<ShopCarItemRespDTO> itemList = new ArrayList<>();
            skuInfoPOS.forEach(x -> {
                ShopCarItemDTO shopCarItemDTO = skuCntMap.get(x.getSkuId());
                ShopCarItemRespDTO item = new ShopCarItemRespDTO();
                item.setCount(shopCarItemDTO.getQty()); // 数量
                item.setSkuInfoDTO(ConvertBeanUtils.convert(x, SkuInfoDTO.class)); // 商品详细信息
                item.setRoomId(shopCarItemDTO.getRoomId()); // 来源直播间
                item.setSelected(shopCarItemDTO.isSelected()); // 是否要进行购买
                itemList.add(item);
            });
            LOGGER.info("itemList" + itemList);
            shopCarRespDTO.setSkuCarItemRespDTOS(itemList);
            return shopCarRespDTO;
        }

        // 查询db进行缓存重建
        List<CartItemRespDTO> cartItemDTOS = cartItemService.selectByUserIdByDB(userId);
        if (CollectionUtils.isEmpty(cartItemDTOS)) {
            // 解决一下缓存击穿
            redisTemplate.opsForValue().set(emptyKey, -1, 5, TimeUnit.MINUTES); // 空标记
            redisTemplate.expire(cacheKey, 5, TimeUnit.MINUTES); // 5分钟后自动删除，防止长期缓存空结果
            return new ShopCarRespDTO();
        }
        // 提取所有涉及的 SKU ID，准备批量查询商品详情
        List<Long> skuIdList = cartItemDTOS.stream()
                .map(CartItemRespDTO::getSkuId)
                .collect(Collectors.toList());

        // 批量查询 SKU 信息
        List<SkuInfoPO> skuInfoPOS = skuInfoService.queryBySkuIds(skuIdList);

        // 构建一个 SKU ID 到 SKU 信息的映射，方便后续查找
        Map<Long, SkuInfoPO> skuInfoMap = skuInfoPOS.stream()
                .collect(Collectors.toMap(SkuInfoPO::getSkuId, sku -> sku));

        // 走redis补偿
        Map<String, String> hash = new HashMap<>();
        for (CartItemRespDTO dto : cartItemDTOS) {
            ShopCarItemDTO value = new ShopCarItemDTO(
                    dto.getQty(),
                    dto.getSelected() == 1,
                    Math.toIntExact(dto.getRoomId()));
            hash.put(String.valueOf(dto.getSkuId()), JSON.toJSONString(value));
        }
        redisTemplate.opsForHash().putAll(cacheKey, hash);

        // 发送一条mq去进行修改最后访问时间，实现解耦，也避免大数据情况下卡顿现象
        this.sendUpdateLastAtMsg(userId);
        return buildShopCarRespFromDb(cartItemDTOS, skuInfoMap);
    }

    public Boolean updateSelectedFlag(ShopCarReqDTO shopCarReqDTO) {
        Long userId = shopCarReqDTO.getUserId();
        Long skuId = shopCarReqDTO.getSkuId();
        String cacheKey = cacheKeyBuilder.buildUserShopCar(userId);
        Object cartItemObj = redisTemplate.opsForHash().get(cacheKey, String.valueOf(skuId));
        if (cartItemObj == null) {
            LOGGER.error("商品不存在, skuId is {}, userId is {}", skuId, userId);
            return false;
        }
        ShopCarItemDTO shopCarItemDTO = JSON.parseObject((String) cartItemObj, ShopCarItemDTO.class);
        boolean newSelectedStatus = !shopCarItemDTO.isSelected(); // 新的选择状态
        shopCarItemDTO.setSelected(newSelectedStatus);
        // 塞回去
        redisTemplate.opsForHash().put(cacheKey, String.valueOf(skuId),
                JSON.toJSONString(shopCarItemDTO));

        // 异步同步数据库
        CartChangeEventBO cartChangeEventBO = new CartChangeEventBO();
        cartChangeEventBO.setUserId(userId);
        cartChangeEventBO.setSkuId(skuId);
        cartChangeEventBO.setQty(shopCarItemDTO.getQty());
        cartChangeEventBO.setSelected(newSelectedStatus);
        cartChangeEventBO.setRoomId(shopCarItemDTO.getRoomId());
        this.sendSyncMsg(cartChangeEventBO);
        return true;
    }




    /**
     * 对冷用户购物车移除缓存
     * @return
     */
    public void removeColdUserCartInCache() {
        List<Long> userIds = cartItemService.selectColdUserId();

        // 直接不用移除
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }

        // 其实这里直接塞到同一个List进行统一移除也是可以的，但是为了更加明显一点，
        // 并且也不要单次传太多的key到redis，所以进行分开存储
        List<String> removeKeys = new ArrayList<>(); // 存储购物车key
        List<String> emptyKeys = new ArrayList<>(); // 存储空值缓存key
        for (Long userId : userIds) {
            removeKeys.add(cacheKeyBuilder.buildUserShopCar(userId)); // 构建要删除的key
            emptyKeys.add(this.buildEmptyKey(userId));
        }

        redisTemplate.delete(removeKeys); // 移除购物车key
        redisTemplate.delete(emptyKeys); // 移除空值缓存key

        int total = removeKeys.size() + emptyKeys.size();
        LOGGER.info("清理冷用户购物车缓存完成，共删除 {} 个 key", total);
    }

    @Override
    public Boolean clearShopCar_v2(ShopCarReqDTO reqVO) {
        String cacheKey = cacheKeyBuilder.buildUserShopCar_v2(reqVO.getUserId(), reqVO.getRoomId());
        redisTemplate.delete(cacheKey);
        return true;
    }

    @Override
    public ShopCarRespDTO getCarInfo_v2(ShopCarReqDTO reqVO) {
        // 获取到redis中当前用户在该直播间的购物车的所有商品信息
        String cacheKey = cacheKeyBuilder.buildUserShopCar_v2(reqVO.getUserId(), reqVO.getRoomId());
        Map<Object, Object> map = redisTemplate.opsForHash().entries(cacheKey);

        // 还没有购物车数据
        if (map.isEmpty()) return new ShopCarRespDTO();


        List<ShopCarItemRespDTO> shopCarItemRespDTOS = new ArrayList<>(); // 当前直播间购物车中的每个商品数据
        Map<Long, Integer> cntMap = new HashMap<>(); // 每个商品的数量
        List<Long> skuIds = new ArrayList<>();

        // 收集skuId和数量
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Long skuId = Long.valueOf(entry.getKey().toString());
            Integer qty = (Integer) entry.getValue();
            if (qty == null || qty <= 0) continue;
            cntMap.put(skuId, qty);
            skuIds.add(skuId);
        }

        // 为空的话直接提前返回
        if (skuIds.isEmpty()) {
            ShopCarRespDTO shopCarRespDTO = new ShopCarRespDTO();
            shopCarRespDTO.setUserId(reqVO.getUserId());
            shopCarRespDTO.setRoomId(reqVO.getRoomId());
            return shopCarRespDTO;
        }

        // 获取所有商品信息
        List<SkuInfoPO> skuInfoPOS = skuInfoService.queryBySkuIds(skuIds);
        for (SkuInfoPO skuInfoPO : skuInfoPOS) {
            ShopCarItemRespDTO shopCarItemRespDTO = new ShopCarItemRespDTO();
            shopCarItemRespDTO.setCount(cntMap.get(skuInfoPO.getSkuId()));
            shopCarItemRespDTO.setSkuInfoDTO(ConvertBeanUtils.convert(skuInfoPO, SkuInfoDTO.class));
            shopCarItemRespDTOS.add(shopCarItemRespDTO);
        }

        // 封装ShopCarRespDTO进行返回
        ShopCarRespDTO shopCarRespDTO = new ShopCarRespDTO();
        shopCarRespDTO.setUserId(reqVO.getUserId());
        shopCarRespDTO.setRoomId(reqVO.getRoomId());
        shopCarRespDTO.setSkuCarItemRespDTOS(shopCarItemRespDTOS);
        return shopCarRespDTO;
    }

    @Override
    public Boolean removeFromCar_v2(ShopCarReqDTO reqVO) {
        String cacheKey = cacheKeyBuilder.buildUserShopCar_v2(reqVO.getUserId(), reqVO.getRoomId());
        LOGGER.info("[removeFromCar_v2] 要开始执行从购物车移除操作，reqVO is {}", reqVO);
        redisTemplate.opsForHash().delete(cacheKey, String.valueOf(reqVO.getSkuId()));
        return true;
    }

    @Override
    public Boolean addCar_v2(ShopCarReqDTO reqVO) {
        String cacheKey = cacheKeyBuilder.buildUserShopCar_v2(reqVO.getUserId(), reqVO.getRoomId());
        redisTemplate.opsForHash().put(cacheKey, String.valueOf(reqVO.getSkuId()), 1);
        return true;
    }


    /**
     * 修改购物车中某一个商品的数量
     * @param reqVO
     * @param changeType (1加，2减)
     * @return
     */
    public Boolean changeCarItemNum_v2(ShopCarReqDTO reqVO, Integer changeType) {
        // 不是指定的操作类型
        if (!SkuNumChangeTypeEnum.isContain(changeType)) return false;

        String cacheKey = cacheKeyBuilder.buildUserShopCar_v2(reqVO.getUserId(), reqVO.getRoomId());

        // 增加/减少 数量
        if (changeType == SkuNumChangeTypeEnum.ADD_NUM.getCode()) {
            redisTemplate.opsForHash().increment(cacheKey, String.valueOf(reqVO.getSkuId()), 1);
        } else if (changeType == SkuNumChangeTypeEnum.SUB_NUM.getCode()) {
            redisTemplate.opsForHash().increment(cacheKey, String.valueOf(reqVO.getSkuId()), -1);
        }
        return true;
    }

    /**
     * 发送同步消息
     * @param cartChangeEventBO
     */
    private void sendSyncMsg(CartChangeEventBO cartChangeEventBO) {
        Message message = new Message();
        message.setTopic(ShopCarProviderTopicNames.SYNC_MSG_TOPIC);
        message.setBody(JSON.toJSONBytes(cartChangeEventBO));
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("发送购物车信息同步消息成功，{}", sendResult);
        } catch (Exception e) {
            LOGGER.info("发送购物车信息同步消息失败", e);
        }
    }


    /**
     * 发送清空购物车消息
     * @param cartChangeEventBO
     */
    private void sendClearMsg(CartChangeEventBO cartChangeEventBO) {
        Message message = new Message();
        message.setTopic(ShopCarProviderTopicNames.CLEAR_CAR_MSG_TOPIC);
        message.setBody(JSON.toJSONBytes(cartChangeEventBO));
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("发送购物车信息清空消息成功，{}", sendResult);
        } catch (Exception e) {
            LOGGER.info("发送购物车信息清空消息失败", e);
        }
    }

    /**
     * 发送修改最后访问时间消息
     * @param userId
     */
    private void sendUpdateLastAtMsg(Long userId) {
        Message message = new Message();
        message.setTopic(ShopCarProviderTopicNames.UPDATE_LAST_LIVE_AT_MSG_TOPIC);
        message.setBody(JSON.toJSONBytes(userId));
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("发送修改最后访问时间消息成功，{}", sendResult);
        } catch (Exception e) {
            LOGGER.info("发送修改最后访问时间消息失败", e);
        }
    }


    /**
     * 辅助方法：根据数据库查到的购物车项 + SKU 信息映射，构建返回 DTO
     * @param cartItemDTOS
     * @param skuInfoMap
     * @return
     */
    private ShopCarRespDTO buildShopCarRespFromDb(List<CartItemRespDTO> cartItemDTOS, Map<Long, SkuInfoPO> skuInfoMap) {
        ShopCarRespDTO resp = new ShopCarRespDTO();
        Long userId = cartItemDTOS.get(0).getUserId(); // 假设不为空，取第一个用户的ID
        resp.setUserId(userId);

        List<ShopCarItemRespDTO> itemList = new ArrayList<>();
        for (CartItemRespDTO dto : cartItemDTOS) {
            SkuInfoPO skuInfo = skuInfoMap.get(dto.getSkuId());
            if (skuInfo == null) {
                continue; // 如果某个 SKU 查询不到，跳过（或可记录日志）
            }

            ShopCarItemRespDTO item = new ShopCarItemRespDTO();
            item.setCount(dto.getQty());
            item.setSkuInfoDTO(ConvertBeanUtils.convert(skuInfo, SkuInfoDTO.class)); // 假设你有这个转换方法
            item.setRoomId(Math.toIntExact(dto.getRoomId()));
            item.setSelected(dto.getSelected() == 1);

            itemList.add(item);
        }

        resp.setSkuCarItemRespDTOS(itemList);
        return resp;
    }


    /**
     * 缓存空值标志
     * @param userId
     * @return
     */
    private String buildEmptyKey(Long userId) {
        return cacheKeyBuilder.buildUserShopCar(userId) + "_NULL";
    }

    /**
     * 删除空值标志
     * @param userId
     * @return
     */
    private boolean deleteEmptyKey(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.delete(this.buildEmptyKey(userId)));
    }

}
