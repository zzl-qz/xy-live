package org.xy.live.gitf.provider.config;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.xy.live.framework.redis.starter.key.GiftProviderCacheKeyBuilder;
import org.xy.live.gift.rpc.ISkuStockInfoRpc;
import org.xy.live.gitf.provider.service.IAnchorShopInfoService;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
public class RefreshStockNumConfig {

    private final static Logger LOGGER = LoggerFactory.getLogger(RefreshStockNumConfig.class);

    @Resource
    private ISkuStockInfoRpc skuStockInfoRpc;
    @Resource
    private IAnchorShopInfoService anchorShopInfoService;
    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private GiftProviderCacheKeyBuilder cacheKeyBuilder;

    @Scheduled(cron = "*/50 * * * * ? ")
    public void refreshStockNum() {
        String lockKey = cacheKeyBuilder.buildStockSyncLock();
        Boolean isLock = redisTemplate.opsForValue().setIfAbsent(lockKey, 1, 15L, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(isLock)) {
            LOGGER.info("[refreshStockNum] 开始执行库存同步到数据库");
            List<Long> anchorIdList = anchorShopInfoService.queryAllValidAnchorId().stream().distinct().toList();
            LOGGER.info("[refreshStockNum] 要进行同步的anchorIdList is {}", anchorIdList);
            for (Long anchorId : anchorIdList) {
                skuStockInfoRpc.syncStockNumToMySQL(anchorId);
            }
        }
    }
}