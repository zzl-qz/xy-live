package org.xy.live.living.provider.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.xy.live.framework.redis.starter.key.LivingProviderCacheKeyBuilder;
import org.xy.live.living.interfaces.constants.LivingRoomTypeEnum;
import org.xy.live.living.interfaces.dto.LivingRoomRespDTO;
import org.xy.live.living.provider.service.ILivingRoomService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用于定期刷新Redis中缓存的直播间列表的list集合(通过延迟线程池)
 */
@Component
public class RefreshLivingRoomListJob{
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshLivingRoomListJob.class);
    
    @Resource
    private ILivingRoomService livingRoomService;
    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private LivingProviderCacheKeyBuilder cacheKeyBuilder;

    // 初始延迟3秒，之后每1秒执行一次
    @XxlJob("refreshLivingRoomList")
    public void refreshLivingRoomList() {
        long start = System.currentTimeMillis();
        LOGGER.info("[refreshLivingRoomList] 任务开始执行，时间戳: {}", start);
        try {
            String cacheKey = cacheKeyBuilder.buildRefreshLivingRoomListLock();
            // 分布式锁
            Boolean lockStatus = redisTemplate.opsForValue().setIfAbsent(cacheKey, 1, 1, TimeUnit.SECONDS);
            if (lockStatus) {
                LOGGER.info("开始同步数据库数据到Redis");
                refreshDBTiRedis(LivingRoomTypeEnum.DEFAULT_LIVING_ROOM.getCode());
                refreshDBTiRedis(LivingRoomTypeEnum.PK_LIVING_ROOM.getCode());
                LOGGER.info("同步完成");
            } else {
                LOGGER.info("未获取到锁，跳过本次执行");
            }
        } catch (Exception e) {
            LOGGER.error("[refreshLivingRoomList] 任务执行异常", e);
        } finally {
            long end = System.currentTimeMillis();
            LOGGER.info("[refreshLivingRoomList] 任务执行结束，耗时: {}ms", end - start);
        }
    }


    private void refreshDBTiRedis(Integer type) {
        String cacheKey = cacheKeyBuilder.buildLivingRoomList(type);
        List<LivingRoomRespDTO> resultList = livingRoomService.listAllLivingRoomFromDB(type);
        if (CollectionUtils.isEmpty(resultList)) {
            // 数据库中没有直播间信息，直接删除 Redis 中对应的缓存键
            redisTemplate.delete(cacheKey);
            return;
        }
        // 打印出来
        LOGGER.info("直播间信息是：" + resultList);
        // 临时key
        String tempListName = cacheKey + "_temp";
        //需要一行一行push进去，pushAll方法有bug，会添加到一条记录里去
        for (LivingRoomRespDTO livingRoomRespDTO : resultList) {
            redisTemplate.opsForList().rightPush(tempListName, livingRoomRespDTO);
        }
        //直接修改重命名这个list，不要直接对原来的list进行修改，减少阻塞的影响
        redisTemplate.rename(tempListName, cacheKey); // 如果 cacheKey已存在，RENAME会直接覆盖它（旧数据会被清除）
        redisTemplate.delete(tempListName); // 删除临时key
    }
}