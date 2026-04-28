package org.xy.live.gitf.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.framework.redis.starter.key.GiftProviderCacheKeyBuilder;
import org.xy.live.gift.dto.GiftConfigDTO;
import org.xy.live.gitf.provider.dao.mapper.GiftConfigMapper;
import org.xy.live.gitf.provider.dao.po.GiftConfigPO;
import org.xy.live.gitf.provider.service.IGiftConfigService;
import org.xy.live.common.interfaces.enums.CommonStatusEnum;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class IGiftConfigServiceImpl implements IGiftConfigService {

    @Resource
    private GiftConfigMapper giftConfigMapper;

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private GiftProviderCacheKeyBuilder cacheKeyBuilder;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public GiftConfigDTO getByGiftId(Integer giftId) {
        // 查询数据库
        LambdaQueryWrapper<GiftConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GiftConfigPO::getGiftId, giftId);
        queryWrapper.eq(GiftConfigPO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        queryWrapper.last("limit 1");
        GiftConfigPO giftConfigPO = giftConfigMapper.selectOne(queryWrapper);
        return ConvertBeanUtils.convert(giftConfigPO, GiftConfigDTO.class);
    }

    @Override
    public List<GiftConfigDTO> queryGiftList() {
        // 查询redis（直接查询所有的记录）
        String cacheKey = cacheKeyBuilder.buildGiftListCacheKey();
        List<GiftConfigDTO> giftConfigDTOS = redisTemplate.opsForList().range(cacheKey, 0, -1).stream().map(x -> (GiftConfigDTO) x).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(giftConfigDTOS)) {
            // 判断是否是之前缓存null值的情况
            if (giftConfigDTOS.get(0).getGiftId() == null) {
                return Collections.emptyList();
            }
            return giftConfigDTOS;
        }
        // 加锁
        RLock lock = redissonClient.getLock(cacheKeyBuilder.buildGiftListLockCacheKey());
        try {
            boolean locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            // 获取锁失败直接返回空集合
            try {
                if (!locked) {
                    return Collections.emptyList();
                }

                // 获取锁成功先判断是否已经重建缓存成功
                giftConfigDTOS = redisTemplate.opsForList().range(cacheKey, 0, -1).stream().map(x -> (GiftConfigDTO) x).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(giftConfigDTOS) && giftConfigDTOS.get(0).getGiftId() != null) {
                    return giftConfigDTOS;
                }

                // 否则就进行缓存重建
                // 查询数据库
                giftConfigDTOS = queryGiftListByDB();
                // 缓存null值，避免穿透的情况
                if (CollectionUtils.isEmpty(giftConfigDTOS)) {
                    redisTemplate.opsForList().leftPush(cacheKey, new GiftConfigDTO());
                    redisTemplate.expire(cacheKey, 1L, TimeUnit.MINUTES);
                    return Collections.emptyList();
                }
                // 补偿
                redisTemplate.opsForList().leftPushAll(cacheKey, giftConfigDTOS.toArray());
                redisTemplate.expire(cacheKey, 30L, TimeUnit.MINUTES);
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return giftConfigDTOS;
    }

    /**
     * 从数据库中查询出所有礼物信息
     * @return
     */
    private List<GiftConfigDTO> queryGiftListByDB() {
        LambdaQueryWrapper<GiftConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GiftConfigPO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        List<GiftConfigPO> giftConfigPOList = giftConfigMapper.selectList(queryWrapper);
        return ConvertBeanUtils.convertList(giftConfigPOList, GiftConfigDTO.class);
    }

    @Override
    public void insertOne(GiftConfigDTO giftConfigDTO) {
        GiftConfigPO giftConfigPO = ConvertBeanUtils.convert(giftConfigDTO, GiftConfigPO.class);
        giftConfigPO.setStatus(CommonStatusEnum.VALID_STATUS.getCode());
        giftConfigMapper.insert(giftConfigPO);
    }

    @Override
    public void updateOne(GiftConfigDTO giftConfigDTO) {
        GiftConfigPO giftConfigPO = ConvertBeanUtils.convert(giftConfigDTO, GiftConfigPO.class);
        giftConfigMapper.updateById(giftConfigPO);
    }
}
