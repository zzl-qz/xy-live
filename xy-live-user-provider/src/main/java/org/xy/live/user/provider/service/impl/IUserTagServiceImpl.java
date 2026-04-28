package org.xy.live.user.provider.service.impl;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.common.message.Message ;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.framework.redis.starter.key.UserProviderCacheKeyBuilder;
import org.xy.live.user.constants.CacheAsyncDeleteCode;
import org.xy.live.user.constants.UserTagFieldNameConstants;
import org.xy.live.user.constants.UserTagsEnum;
import org.xy.live.user.dto.UserCacheAsyncDeleteDT0;
import org.xy.live.user.dto.UserTagDTO;
import org.xy.live.user.provider.dao.mapper.IUserTagMapper;
import org.xy.live.user.provider.dao.po.UserTagPo;
import org.xy.live.user.provider.service.IUserTagService;
import org.xy.live.user.utils.UserTagUtils;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.xy.live.user.constants.UserProviderTopicNames.CACHE_ASYNC_DELETE_TOPIC;

@Service
public class IUserTagServiceImpl implements IUserTagService {

    @Resource
    private IUserTagMapper userTagMapper;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserProviderCacheKeyBuilder userProviderCacheKeyBuilder;

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, UserTagDTO> redisTemplate;

    @Resource
    private MQProducer mqProducer;

    /**
     * 设置标签
     * @param userId
     * @param userTagsEnum
     * @return
     */
    @Override
    public boolean setTag(Long userId, UserTagsEnum userTagsEnum) {

        boolean updateSuccess = userTagMapper.setTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
        if (updateSuccess) {
            deleteUserTagDT0FromRedis(userId); // 设置成功就删除一下缓存
            return true;
        }

        // 如果上面执行失败有两种情况：一是原本没有这个用户标签，二是真的失败（这两种情况都不需要删除缓存）
        try {
            // 这里可能在并发场景下会导致都查询到null的情况，然后去执行插入操作，加个分布式锁解决一下
            RLock lock = redissonClient.getLock(userProviderCacheKeyBuilder.buildTagLockKey(userId));
            boolean locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                // 获取锁失败,直接返回false
                return false;
            }
            // 拿锁成功的去查询数据库
            // 如果这里返回false可能会有两种场景，
            // 第一种就是真的失败，第二种就是原先是没有这条数据的（需要判断一下）
            UserTagPo userTagPo = userTagMapper.selectById(userId);
            if (userTagPo != null) { // 是真的失败
                return false;
            }
            try {
                // 获取锁成功，补偿一下（先插入数据，再进行修改）
                userTagPo = new UserTagPo();
                userTagPo.setUserId(userId);
                userTagMapper.insert(userTagPo);
                updateSuccess = userTagMapper.setTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
            } finally {
                // 确保锁被释放
                lock.unlock();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return updateSuccess;
    }

    /**
     * 取消标签
     * @param userId
     * @param userTagsEnum
     * @return
     */
    @Override
    public boolean cancelTag(Long userId, UserTagsEnum userTagsEnum) {
        boolean cancelSuccess = userTagMapper.cancelTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
        if (!cancelSuccess) {
            return false;
        }

        // 取消成功的话还需要去删除缓存
        deleteUserTagDT0FromRedis(userId);
        return true;
    }


    /**
     * 当前是否包含某个标签
     * @param userId
     * @param userTagsEnum
     * @return
     */
    @Override
    public boolean containTag(Long userId, UserTagsEnum userTagsEnum) {
        // 先查询出当前用户
        if (userId == null) {
            return false;
        }

        // 从redis中查询一下是否存在
        UserTagDTO userTagDTO = queryByUserIdFromRedis(userId);
        if (userTagDTO == null) {
            return false;
        }

        String fieldName = userTagsEnum.getFieldName();
        if (UserTagFieldNameConstants.TAT_INFO_01.equals(fieldName)) {
            return UserTagUtils.isContain(userTagDTO.getTagInfo01(), userTagsEnum.getTag());
        } else if (UserTagFieldNameConstants.TAT_INFO_02.equals(fieldName)) {
            return UserTagUtils.isContain(userTagDTO.getTagInfo02(), userTagsEnum.getTag());
        } else if (UserTagFieldNameConstants.TAT_INFO_03.equals(fieldName)) {
            return UserTagUtils.isContain(userTagDTO.getTagInfo03(), userTagsEnum.getTag());
        }
        return false;
    }

    /**
     * 判断redis中是否有相应的数据
     * @param userId
     * @return
     */
    private UserTagDTO queryByUserIdFromRedis(Long userId) {
        // 先查询缓存是有没有
        String cacheKey = userProviderCacheKeyBuilder.buildUserTagKey(userId);
        UserTagDTO userTagDTO = redisTemplate.opsForValue().get(cacheKey);
        if (userTagDTO != null) {
            // 是否是缓存的空值
            if (userTagDTO.getUserId() != null) return userTagDTO;
        }

        // 获取锁
        RLock lock = redissonClient.getLock(userProviderCacheKeyBuilder.buildGetUserTagDTOLockKey(userId));
        try {
            boolean locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                // 获取锁失败,直接返回null
                return null;
            }

            // 进行二次判断，可能不是第一个获取到锁的，别人已经构建好缓存了
            userTagDTO = redisTemplate.opsForValue().get(cacheKey);
            if (userTagDTO != null) {
                // 是否是缓存的空值
                if (userTagDTO.getUserId() != null) return userTagDTO;
            }

            // 查询数据库
            UserTagPo userTagPo = userTagMapper.selectById(userId);
            if (userTagPo == null) {
                // 缓存空值
                redisTemplate.opsForValue().set(cacheKey, new UserTagDTO(), 5L, TimeUnit.MINUTES);
                return null;
            }

            // 将数据缓存到redis中
            userTagDTO = ConvertBeanUtils.convert(userTagPo, UserTagDTO.class);
            redisTemplate.opsForValue().set(cacheKey, userTagDTO, 30L, TimeUnit.MINUTES);
            return userTagDTO;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock(); // 释放锁
        }
    }


    /**
     * 删除缓存(带延迟删除逻辑)
     * @param userId
     */
    private void deleteUserTagDT0FromRedis(Long userId) {
        String cacheKey = userProviderCacheKeyBuilder.buildUserTagKey(userId);
        redisTemplate.delete(cacheKey);
        // 延迟删除逻辑
        try {
            Message message = new Message();
            UserCacheAsyncDeleteDT0 userCacheAsyncDeleteDT0 = new UserCacheAsyncDeleteDT0();
            userCacheAsyncDeleteDT0.setCode(CacheAsyncDeleteCode.USER_TAG_DELETE.getCode());
            HashMap<String, Object> jsonParam = new HashMap<>();
            jsonParam.put("userId", userId);
            userCacheAsyncDeleteDT0.setJson(JSON.toJSONString(jsonParam));
            message.setBody(JSON.toJSONString(userCacheAsyncDeleteDT0).getBytes());
            message.setTopic(CACHE_ASYNC_DELETE_TOPIC);
            //延迟级别，1代表延迟一秒发送
            message.setDelayTimeLevel(1);
            mqProducer.send(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
