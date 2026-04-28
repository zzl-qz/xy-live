package org.xy.live.gitf.provider.rpc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.xy.live.bank.interfaces.dto.PayProductDTO;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.framework.redis.starter.key.GiftProviderCacheKeyBuilder;
import org.xy.live.gift.dto.SkuDetailInfoDTO;
import org.xy.live.gift.dto.SkuInfoDTO;
import org.xy.live.gift.rpc.ISkuInfoRpc;
import org.xy.live.gitf.provider.dao.mapper.ISkuInfoMapper;
import org.xy.live.gitf.provider.dao.po.SkuInfoPO;
import org.xy.live.gitf.provider.service.IAnchorShopInfoService;
import org.xy.live.gitf.provider.service.ISkuInfoService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@DubboService
public class SkuInfoRpcImpl implements ISkuInfoRpc {


    @Resource
    private GiftProviderCacheKeyBuilder cacheKeyBuilder;

    @Resource
    private IAnchorShopInfoService anchorShopInfoService;

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Objects> redisTemplate;

    @Resource
    private ISkuInfoService skuInfoService;

    @Override
    public List<SkuInfoDTO> queryByAnchorId(Long anchorId) {
        // 以主播为维度进行隔离
        String cacheKey = cacheKeyBuilder.buildSkuDetailInfoMap(anchorId);
        // 先查询redis中是否存在
        List<SkuInfoDTO> skuInfoDTOS = redisTemplate.opsForHash().values(cacheKey).stream().map(x -> (SkuInfoDTO) x).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(skuInfoDTOS)) {
            if (skuInfoDTOS.get(0).getSkuId() == null) {
                return Collections.emptyList();
            }
            return skuInfoDTOS;
        }

        // 开始进行db操作
        // 根据主播Id查询出一批商品Id
        List<Long> skuIdList = anchorShopInfoService.querySkuIdsByAnchorId(anchorId);
        System.out.println("skuIdList" + skuIdList);
        if (CollectionUtils.isEmpty(skuIdList)) {
            return Collections.emptyList();
        }

        // 再根据这一批商品Id查询出详细的商品信息
        skuInfoDTOS = ConvertBeanUtils.convertList(skuInfoService.queryBySkuIds(skuIdList), SkuInfoDTO.class);
        if (CollectionUtils.isEmpty(skuInfoDTOS)) {
            redisTemplate.opsForHash().put(cacheKey, "-1", new PayProductDTO());
            redisTemplate.expire(cacheKey, 1L, TimeUnit.MINUTES);
            return Collections.emptyList();
        }

        // 使用Redis进行缓存
        Map<String, SkuInfoDTO> skuInfoMap = skuInfoDTOS.stream().collect(Collectors.toMap(x -> String.valueOf(x.getSkuId()), x -> x));
        // 当使用putAll往hash结构中塞入map集合时，会自动将map的key转化成filed，将map的value转成value
        redisTemplate.opsForHash().putAll(cacheKey, skuInfoMap);
        redisTemplate.expire(cacheKey, 30L, TimeUnit.MINUTES);
        return skuInfoDTOS;
    }

    @Override
    public SkuDetailInfoDTO queryBySkuId(Long skuId, Long anchorId) {
        // 查询缓存
        String cacheKey = cacheKeyBuilder.buildSkuDetailInfoMap(anchorId);
        SkuInfoDTO skuInfoDTO = (SkuInfoDTO) redisTemplate.opsForHash().get(cacheKey, String.valueOf(skuId));
        if (skuInfoDTO != null) {
            return ConvertBeanUtils.convert(skuInfoDTO, SkuDetailInfoDTO.class);
        }
        // 查询db
        SkuInfoPO skuInfoPO = skuInfoService.queryBySkuId(skuId);
        if (skuInfoPO == null) {
           return null;
        }
        // 补偿
        skuInfoDTO = ConvertBeanUtils.convert(skuInfoPO, SkuInfoDTO.class);
        redisTemplate.opsForHash().put(cacheKey, String.valueOf(skuId), skuInfoDTO);
        redisTemplate.expire(cacheKey, 30, TimeUnit.MINUTES);
        return ConvertBeanUtils.convert(skuInfoDTO, SkuDetailInfoDTO.class);
    }
}
