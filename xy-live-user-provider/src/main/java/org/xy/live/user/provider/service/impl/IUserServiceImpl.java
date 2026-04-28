package org.xy.live.user.provider.service.impl;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.framework.redis.starter.key.UserProviderCacheKeyBuilder;
import org.xy.live.user.constants.CacheAsyncDeleteCode;
import org.xy.live.user.dto.UserCacheAsyncDeleteDT0;
import org.xy.live.user.dto.UserDTO;
import org.xy.live.user.provider.dao.mapper.IUserMapper;
import org.xy.live.user.provider.dao.po.UserPO;
import org.xy.live.user.provider.service.IUserService;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.xy.live.user.constants.UserProviderTopicNames.CACHE_ASYNC_DELETE_TOPIC;


@Service
public class IUserServiceImpl implements IUserService {

    @Resource
    private IUserMapper userMapper;

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, UserDTO> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    // @Resource
    private MQProducer mqProducer;

    @Resource
    private UserProviderCacheKeyBuilder userProviderCacheKeyBuilder;

    // 布隆过滤器（redission实现）
    private RBloomFilter<Long> userBloomFilter;

    /**
     * 对布隆过滤器进行初始化
     */
    @PostConstruct
    public void initBloomFilter() {
        // 初始化Redisson布隆过滤器
        userBloomFilter = redissonClient.getBloomFilter("user:id:bloom:filter");

        // 参数说明：预期元素数量1,000,000，误判率0.01%
        // 注意：Redisson的误判率参数是小数形式（0.0001表示0.01%）
        userBloomFilter.tryInit(1_000_000L, 0.0001);

        // 初始化数据
        List<Long> allUserIds = userMapper.selectAllUserIdsFromIndex();
        allUserIds.forEach(userBloomFilter::add); // 不需要并行，Redisson会批量处理
        System.out.println("执行到这里");
    }


    /**
     * 根据用户id获取DTO对象
     * @param userId
     * @return
     */
    @Override
    public UserDTO getByUserId(Long userId) {
        System.out.println("userId" + userId);
        if (userId == null) {
            return null;
        }

        // 查询是否在布隆过滤器里面(没有就是没有，有不一定有)
        if (!userBloomFilter.contains(userId)) {
            return null;
        }
        // 拼接出key
        String key = userProviderCacheKeyBuilder.buildUserInfoKey(userId);
        // 判断缓存中有没有
        UserDTO userDTO = redisTemplate.opsForValue().get(key);
        if (userDTO != null) {
            if (userDTO.getUserId() != null) return userDTO;
        }

        RLock lock = redissonClient.getLock(userProviderCacheKeyBuilder.buildUserInfoLockKey(userId));
        try {
            // 尝试获取锁，等待5秒，锁自动释放时间30秒
            boolean locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                // 获取锁失败，可以再次检查缓存(有就返回，没有就是返回null)
                return redisTemplate.opsForValue().get(key);
            }

            try {
                // 3. 双重检查缓存
                userDTO = redisTemplate.opsForValue().get(key);
                if (userDTO != null) {
                    if (userDTO.getUserId() != null) return userDTO;
                }

                // 4. 查询数据库
                userDTO = ConvertBeanUtils.convert(userMapper.selectById(userId), UserDTO.class);
                if (userDTO != null) {
                    // 5. 写入缓存（添加随机过期时间防止雪崩）
                    redisTemplate.opsForValue().set(key, userDTO, createRandomExpireTime(), TimeUnit.SECONDS);
                    return userDTO;
                }
                // 使用降级方案（缓存null）
                redisTemplate.opsForValue().set(key, new UserDTO(), createRandomExpireTime(), TimeUnit.SECONDS);
                return null;
            } finally {
                // 确保锁被释放
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁被中断", e);
        } catch (Exception e) {
            throw new RuntimeException("查询用户信息失败", e);
        }
    }


    /**
     * 修改用户信息
     * @param userDTO
     * @return
     */
    @Override
    public boolean updateUserInfo(UserDTO userDTO) {
        if (userDTO == null || userDTO.getUserId() == null) {
            return false;
        }
        // 删除缓存
        String key = userProviderCacheKeyBuilder.buildUserInfoKey(userDTO.getUserId());
        redisTemplate.delete(key);
        // 改数据库
        userMapper.updateById(ConvertBeanUtils.convert(userDTO, UserPO.class));
        try {
            //发送延迟消息，触发二次删除缓存操作
            Message message = new Message();
            UserCacheAsyncDeleteDT0 userCacheAsyncDeleteDT0 = new UserCacheAsyncDeleteDT0();
            userCacheAsyncDeleteDT0.setCode(CacheAsyncDeleteCode.USER_INFO_DELETE.getCode());
            HashMap<String, Object> jsonParam = new HashMap<>();
            jsonParam.put("userId", userDTO.getUserId());
            userCacheAsyncDeleteDT0.setJson(JSON.toJSONString(jsonParam));
            message.setBody(JSON.toJSONString(userCacheAsyncDeleteDT0).getBytes());
            message.setTopic(CACHE_ASYNC_DELETE_TOPIC);
            //延迟级别，1代表延迟一秒发送
            message.setDelayTimeLevel(1);
            mqProducer.send(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }


    /**
     * 新增一条数据
     * @param userDTO
     * @return
     */
    @Override
    public boolean insertOne(UserDTO userDTO) {
        if (userDTO == null || userDTO.getUserId() == null) {
            return false;
        }
        userMapper.insert(ConvertBeanUtils.convert(userDTO, UserPO.class));

        // 将用户Id插入全局索引表
        userMapper.insertToIndexTable(userDTO.getUserId());

        // 新增用户ID加入布隆过滤器
        userBloomFilter.add(userDTO.getUserId());

        return true;
    }


    /**
     * 批量查询用户信息
     * @param userIdList
     * @return
     */
    @Override
    public Map<Long, UserDTO> batchQueryUserInfo(List<Long> userIdList) {
        // 判断是否为空
        if(CollectionUtils.isEmpty(userIdList)) {
            return Collections.emptyMap();
        }
        // 先过滤掉布隆过滤器不存在的ID
        userIdList = userIdList.stream()
                .filter(id -> id > 10000 && userBloomFilter.contains(id))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(userIdList)) {
            return Collections.emptyMap();
        }
        // 过滤条件：user的id都大于10000
        userIdList = userIdList.stream().filter(id -> id > 10000).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(userIdList)) {
            return Collections.emptyMap();
        }

        // redis
        List<String> keyList = new ArrayList<>();
        userIdList.forEach(userId -> {
            keyList.add(userProviderCacheKeyBuilder.buildUserInfoKey(userId));
        });
        // 使用批量获取的方式，并且过滤掉为null的情况
        List<UserDTO> userDTOList =
                redisTemplate.opsForValue().multiGet(keyList).stream().filter(x -> x != null).collect(Collectors.toList());
        // 全部命中的情况
        if (!CollectionUtils.isEmpty(userDTOList) && userDTOList.size() == userIdList.size()) {
            return userDTOList.stream().collect(Collectors.toMap(UserDTO::getUserId, x->x));
        }
        // 筛选出没命中的userId
        List<Long> userIdINCacheList = userDTOList.stream().map(UserDTO::getUserId).collect(Collectors.toList());
        List<Long> userIdNOTINCacheList = userIdList.stream().filter(x -> !userIdINCacheList.contains(x)).collect(Collectors.toList());

        // 多线程替换 union all
        // 因为有100张分表，所以使用分组的方式（防止sharding-jdbc笛卡尔积路由）
        Map<Long, List<Long>> userIdMap = userIdNOTINCacheList.stream().collect(Collectors.groupingBy(userId -> (userId % 100)));
        List<UserDTO> dbQueryList = new CopyOnWriteArrayList<>();
        // 多线程查询
        userIdMap.values().parallelStream().forEach(queryUserIdList -> {
            dbQueryList.addAll(ConvertBeanUtils.convertList(userMapper.selectBatchIds(queryUserIdList), UserDTO.class));
        });
        if (!CollectionUtils.isEmpty(dbQueryList)) {
            // redis补偿
            Map<String, UserDTO> cacheMap = dbQueryList.stream().collect(Collectors.toMap(userDTO -> userProviderCacheKeyBuilder.buildUserInfoKey(userDTO.getUserId()), x -> x));
            redisTemplate.opsForValue().multiSet(cacheMap);
            // mset是不能设置过期时间，可以使用管道设置，批量处理，减少网路IO
            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                    for (String key : cacheMap.keySet()) {
                        operations.expire((K)key, createRandomExpireTime(), TimeUnit.SECONDS);
                    }
                    return null;
                }
            });
            // 两部分查询结果整合
            userDTOList.addAll(dbQueryList);
        }
        return userDTOList.stream().collect(Collectors.toMap(UserDTO::getUserId, x->x));
    }

    /**
     * 生成随机过期时间，单位：秒
     * @return
     */
    private long createRandomExpireTime() {
        return ThreadLocalRandom.current().nextLong(1000) + 60 * 30;//30min + 随机1000s以内
    }

}
