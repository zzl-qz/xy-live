package org.xy.live.gitf.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.framework.redis.starter.key.GiftProviderCacheKeyBuilder;
import org.xy.live.gift.constants.SkuOrderInfoEnum;
import org.xy.live.gift.dto.PrepareOrderReqDTO;
import org.xy.live.gift.dto.SkuOrderInfoReqDTO;
import org.xy.live.gift.dto.SkuOrderInfoRespDTO;
import org.xy.live.gitf.provider.dao.mapper.ISkuOrderInfoMapper;
import org.xy.live.gitf.provider.dao.po.SkuOrderInfoPO;
import org.xy.live.gitf.provider.service.ISkuOrderInfoService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
public class SkuOrderInfoServiceImpl implements ISkuOrderInfoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkuOrderInfoServiceImpl.class);

    @Resource
    private ISkuOrderInfoMapper skuOrderInfoMapper;

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private GiftProviderCacheKeyBuilder cacheKeyBuilder;

    /**
     * 根据userId和roomId查询订单信息(会查询出所有的订单，包括已经取消和未支付以及支付完的订单)
     * @param userId
     * @return
     */
    @Override
    public List<SkuOrderInfoRespDTO> queryByUserIdAndRoomId(Long userId, Integer roomId) {
        // redis
        String cacheKey = cacheKeyBuilder.buildOrderByUserIdAndRoomId(userId, roomId);
        List<Object> objectList = redisTemplate.opsForList().range(cacheKey, 0, -1);
        if (!objectList.isEmpty()) {
            return ConvertBeanUtils.convertList(objectList, SkuOrderInfoRespDTO.class);
        }

        // db
        LambdaQueryWrapper<SkuOrderInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SkuOrderInfoPO::getUserId, userId);
        queryWrapper.eq(SkuOrderInfoPO::getRoomId, roomId);
        queryWrapper.orderByDesc(SkuOrderInfoPO::getId);
        List<SkuOrderInfoPO> skuOrderInfoPOS = skuOrderInfoMapper.selectList(queryWrapper);
        if (!skuOrderInfoPOS.isEmpty()) {
            List<SkuOrderInfoRespDTO> skuOrderInfoRespDTOS = ConvertBeanUtils.convertList(skuOrderInfoPOS, SkuOrderInfoRespDTO.class);
            redisTemplate.opsForList().leftPushAll(cacheKey, skuOrderInfoRespDTOS.toArray());
            redisTemplate.expire(cacheKey, 1, TimeUnit.HOURS);
            return skuOrderInfoRespDTOS;
        }
        return Collections.emptyList();
    }

    /**
     * 根据userId查询订单信息(会查询出所有的订单，包括已经取消和未支付以及支付完的订单)
     * @param userId
     * @return
     */
    @Override
    public List<SkuOrderInfoRespDTO> queryByUserId(Long userId) {
        // 查询redis
        String cacheKey = cacheKeyBuilder.buildOrderByUserId(userId);
        List<Object> objectList = redisTemplate.opsForList().range(cacheKey, 0, -1);
        if (!objectList.isEmpty()) {
            return ConvertBeanUtils.convertList(objectList, SkuOrderInfoRespDTO.class);
        }

        // 查询db
        LambdaQueryWrapper<SkuOrderInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SkuOrderInfoPO::getUserId, userId);
        queryWrapper.orderByDesc(SkuOrderInfoPO::getId);
        List<SkuOrderInfoPO> skuOrderInfoPOS = skuOrderInfoMapper.selectList(queryWrapper);
        if (!skuOrderInfoPOS.isEmpty()) {
            List<SkuOrderInfoRespDTO> skuOrderInfoRespDTOS = ConvertBeanUtils.convertList(skuOrderInfoPOS, SkuOrderInfoRespDTO.class);
            redisTemplate.opsForList().leftPushAll(cacheKey, skuOrderInfoRespDTOS.toArray());
            redisTemplate.expire(cacheKey, 1, TimeUnit.HOURS);
            return skuOrderInfoRespDTOS;
        }
        return Collections.emptyList();
    }

    @Override
    public SkuOrderInfoPO insertOne(SkuOrderInfoReqDTO skuOrderInfoReqDTO) {
        // 切割 1:3,2:3,5:3 ——> skuId:count
        String skuIdListStr = StringUtil.join(skuOrderInfoReqDTO.getSkuIdList(), ",");

        // 插入
        SkuOrderInfoPO skuOrderInfoPO = ConvertBeanUtils.convert(skuOrderInfoReqDTO, SkuOrderInfoPO.class);
        skuOrderInfoPO.setSkuIdList(skuIdListStr);
        skuOrderInfoMapper.insert(skuOrderInfoPO);

        // 删除缓存
        redisTemplate.delete(cacheKeyBuilder.buildOrderByUserIdAndRoomId(
                skuOrderInfoReqDTO.getUserId(), skuOrderInfoReqDTO.getRoomId()));
        redisTemplate.delete(cacheKeyBuilder.buildOrderByUserId(skuOrderInfoReqDTO.getUserId()));
        return skuOrderInfoPO;
    }


    @Override
    public boolean updateOrderStatus(SkuOrderInfoReqDTO skuOrderInfoReqDTO, Integer oldTypeCode) {
        // 删除缓存
        redisTemplate.delete(cacheKeyBuilder.buildOrderByUserIdAndRoomId(
                skuOrderInfoReqDTO.getUserId(), skuOrderInfoReqDTO.getRoomId()));
        redisTemplate.delete(cacheKeyBuilder.buildOrderByUserId(skuOrderInfoReqDTO.getUserId()));

        // 修改数据库
//        int effectRow = skuOrderInfoMapper.updateOrderStatus(skuOrderInfoReqDTO.getId(),
//                skuOrderInfoReqDTO.getStatus(), SkuOrderInfoEnum.PREPARE_PAY.getCode());
        int effectRow = skuOrderInfoMapper.updateOrderStatus(skuOrderInfoReqDTO.getId(),
                skuOrderInfoReqDTO.getStatus(), oldTypeCode);
        // 修改失败
        if (effectRow == 0) {
            LOGGER.warn("[updateOrderStatus] 执行修改失败， id is {}", skuOrderInfoReqDTO.getId());
            return false;
        }
        return true;
    }

    @Override
    public SkuOrderInfoRespDTO queryByOrderId(Long orderId) {
        LambdaQueryWrapper<SkuOrderInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SkuOrderInfoPO::getId, orderId);
        return ConvertBeanUtils.convert(skuOrderInfoMapper.selectOne(queryWrapper), SkuOrderInfoRespDTO.class);
    }


    @Override
    public boolean judgmentStatus(Long orderId, SkuOrderInfoEnum skuOrderInfoEnum) {
        SkuOrderInfoRespDTO skuOrderInfoRespDTO = this.queryByOrderId(orderId);
        if (skuOrderInfoRespDTO == null || !skuOrderInfoRespDTO.getStatus().equals(skuOrderInfoEnum.getCode())) {
            return false;
        }
        return true;
    }

    @Override
    public List<SkuOrderInfoReqDTO> selectByStatus(int status) {
        LambdaQueryWrapper<SkuOrderInfoPO> qw = new LambdaQueryWrapper<>();
        qw.eq(SkuOrderInfoPO::getStatus, status);
        List<SkuOrderInfoPO> skuOrderInfoPOS = skuOrderInfoMapper.selectList(qw);
        return ConvertBeanUtils.convertList(skuOrderInfoPOS, SkuOrderInfoReqDTO.class);
    }


}
