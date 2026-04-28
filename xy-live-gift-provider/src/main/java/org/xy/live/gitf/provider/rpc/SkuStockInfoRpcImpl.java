package org.xy.live.gitf.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.xy.live.framework.redis.starter.key.GiftProviderCacheKeyBuilder;
import org.xy.live.gift.rpc.ISkuStockInfoRpc;
import org.xy.live.gitf.provider.dao.po.SkuStockInfoPO;
import org.xy.live.gitf.provider.service.IAnchorShopInfoService;
import org.xy.live.gitf.provider.service.ISkuStockInfoService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@DubboService
public class SkuStockInfoRpcImpl implements ISkuStockInfoRpc {


    private final Logger LOGGER = LoggerFactory.getLogger(SkuStockInfoRpcImpl.class);

    @Resource
    private ISkuStockInfoService skuStockInfoService;
    @Resource
    private IAnchorShopInfoService anchorShopInfoService;
    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private GiftProviderCacheKeyBuilder cacheKeyBuilder;

    @Override
    public boolean decrStockNumBySkuId(Long skuId, Integer num) {
        return skuStockInfoService.decrStockNumBySkuIdByLua(skuId, num);
    }

    @Override
    public boolean prepareStockInfo(Long anchorId) {
        // 查看是否已经有加载过的标识
        String prepareFlagKey = cacheKeyBuilder.buildPrepareSkuStockFlag(anchorId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(prepareFlagKey))) {
            LOGGER.warn("[prepareStockInfo] prepareFlagKey 存在，主播已经加载过缓存了");
            return false;
        }

        // 查询出来之后塞到redis中, 根据主播Id查询出skuId集合
        List<Long> skuIds = anchorShopInfoService.querySkuIdsByAnchorId(anchorId);
        LOGGER.info("[prepareStockInfo] skuIds is {}", skuIds);
        List<SkuStockInfoPO> skuStockInfoPOS = skuStockInfoService.queryBySkuIds(skuIds);
        LOGGER.info("[prepareStockInfo] skuStockInfoPOS is {}", skuStockInfoPOS);
        // k-cacheKey    v-数量
        Map<String, Integer> cacheKeyMap = skuStockInfoPOS.stream()
                .collect(Collectors.toMap(skuStockInfoPO ->
                        cacheKeyBuilder.buildSkuStock(skuStockInfoPO.getSkuId()), SkuStockInfoPO::getStockNum));
        LOGGER.info("[cacheKeyMap], {}", cacheKeyMap);
        // 塞到redis中
        redisTemplate.opsForValue().multiSet(cacheKeyMap);
        // 设置过期时间
        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                for (String key : cacheKeyMap.keySet()) {
                    redisTemplate.expire(key, 1, TimeUnit.DAYS);
                }
                return null;
            }
        });

        // 标识一下已经预备过了
        redisTemplate.opsForValue().set(prepareFlagKey, "prepare ok", 12, TimeUnit.HOURS);
        return true;
    }

    @Override
    public Integer queryStockNum(Long skuId) {
        String cacheKey = cacheKeyBuilder.buildSkuStock(skuId);
        Object stockNumObj = redisTemplate.opsForValue().get(cacheKey);
        LOGGER.info("[queryStockNum] skuId is {}, stockNumObj is {}", skuId, stockNumObj);
        return stockNumObj == null ? -1 : (Integer) stockNumObj;
    }

    @Override
    public boolean syncStockNumToMySQL(Long anchor) {
        List<Long> skuIdList = anchorShopInfoService.querySkuIdsByAnchorId(anchor);
        LOGGER.info("[syncStockNumToMySQL]  skuIdList is {}", skuIdList);
        for (Long skuId : skuIdList) {
            Integer stockNum = this.queryStockNum(skuId);
            if (stockNum != null && stockNum != -1) {
                LOGGER.info("[syncStockNumToMySQL]  skuId is {}, stockNum is {}", skuId, stockNum);
                skuStockInfoService.updateStockNum(skuId, stockNum);
            }
        }
        return true;
    }
}
