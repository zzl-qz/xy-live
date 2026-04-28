package org.xy.live.bank.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.xy.live.bank.interfaces.dto.PayProductDTO;
import org.xy.live.bank.provider.dao.mapper.PayProductMapper;
import org.xy.live.bank.provider.dao.po.PayProductPO;
import org.xy.live.bank.provider.service.PayProductService;
import org.xy.live.common.interfaces.enums.CommonStatusEnum;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.framework.redis.starter.key.BankProviderCacheKeyBuilder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PayProductServiceImpl implements PayProductService {

    @Resource
    private BankProviderCacheKeyBuilder bankProviderCacheKeyBuilder;

    @Resource
    private PayProductMapper payProductMapper;

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(PayProductServiceImpl.class);

    @Override
    public List<PayProductDTO> products(Integer type) {
        // 查询redis
        String cacheKey = bankProviderCacheKeyBuilder.buildPayProductCache(type);
        LOGGER.info("[cacheKey] is{}", cacheKey);
        List<PayProductDTO> payProductDTOList = redisTemplate.opsForList()
                .range(cacheKey, 0, 30).stream().map(x -> (PayProductDTO) x).collect(Collectors.toList());
        LOGGER.info("[payProductDTOList] is{}", payProductDTOList);
        if (!CollectionUtils.isEmpty(payProductDTOList)) {
            if (payProductDTOList.get(0).getId() == null) {
                return Collections.emptyList();
            }
            // 续期
            redisTemplate.expire(cacheKey, 1L, TimeUnit.MINUTES);
            return payProductDTOList;
        }


        // 查询db
        LambdaQueryWrapper<PayProductPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PayProductPO::getType, type);
        queryWrapper.eq(PayProductPO::getValidStatus, CommonStatusEnum.VALID_STATUS.getCode());
        queryWrapper.orderByDesc(PayProductPO::getPrice); // 按照价格进行排序
        List<PayProductPO> payProductPOList = payProductMapper.selectList(queryWrapper);
        if (!CollectionUtils.isEmpty(payProductPOList)) {
            // 补偿操作
            payProductDTOList = ConvertBeanUtils.convertList(payProductPOList, PayProductDTO.class);
            // List类型的putAll放入集合有bug，需要转换为数组
            redisTemplate.opsForList().leftPushAll(cacheKey, payProductDTOList.toArray());
            redisTemplate.expire(cacheKey, 1L, TimeUnit.MINUTES);
            return payProductDTOList;
        }


        // 穿透了
        redisTemplate.opsForList().leftPush(cacheKey, new PayProductDTO());
        redisTemplate.expire(cacheKey, 1L, TimeUnit.MINUTES);
        return null;
    }

    @Override
    public PayProductDTO getByProductId(Integer productId) {

        // 思考：为什么明明上面已经有将所有的商品列表存储到redis的集合中，这里还要再存储一遍
        // 因为上面存储到集合中的时候需要传递一个type，站在调用方的角度来说还要额外去传递一个type，肯定不太友好
        // 所以这里采用再存储一份的方式（占内存，但是友好）

        // 先查询redis
        String cacheKey = bankProviderCacheKeyBuilder.buildPayProductItemCache(productId);
        PayProductDTO payProductDTO = (PayProductDTO) redisTemplate.opsForValue().get(cacheKey);
        if (payProductDTO != null) {
            if (payProductDTO.getId() == null) {
                return null;
            }
            return payProductDTO;
        }


        // 查询DB
        LambdaQueryWrapper<PayProductPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PayProductPO::getId, productId);
        queryWrapper.eq(PayProductPO::getValidStatus, CommonStatusEnum.VALID_STATUS.getCode());
        queryWrapper.last("limit 1");
        payProductDTO = ConvertBeanUtils.convert(payProductMapper.selectOne(queryWrapper), PayProductDTO.class);
        // 预防操作
        if (payProductDTO == null) {
            redisTemplate.opsForValue().set(cacheKey, new PayProductDTO(), 1L, TimeUnit.MINUTES);
            return null;
        }


        // 补偿操作
        redisTemplate.opsForValue().set(cacheKey, payProductDTO, 30L, TimeUnit.MINUTES);
        return payProductDTO;
    }
}
