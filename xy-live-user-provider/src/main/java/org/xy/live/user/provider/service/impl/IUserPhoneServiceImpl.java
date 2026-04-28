package org.xy.live.user.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.framework.redis.starter.key.UserProviderCacheKeyBuilder;
import org.xy.live.id.generate.interfaces.IdGenerateRpc;
import org.xy.live.user.dto.UserDTO;
import org.xy.live.user.dto.UserLoginDTO;
import org.xy.live.user.dto.UserPhoneDTO;
import org.xy.live.common.interfaces.enums.CommonStatusEnum;
import org.xy.live.user.provider.dao.mapper.IUserPhoneMapper;
import org.xy.live.user.provider.dao.po.UserPhonePO;
import org.xy.live.user.provider.service.IUserPhoneService;
import org.xy.live.user.provider.service.IUserService;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.xy.live.common.interfaces.utils.AESUtils.encrypt;
import static org.xy.live.id.generate.enums.IdTypeEnum.USER_ID;

@Service
public class IUserPhoneServiceImpl implements IUserPhoneService {

    @Resource
    private IUserPhoneMapper userPhoneMapper;

    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserProviderCacheKeyBuilder userProviderCacheKeyBuilder;

    @Resource
    private IUserService userService;


    @DubboReference
    private IdGenerateRpc idGenerateRpc;


    @Resource
    private RedissonClient redissonClient;

    /**
     * 进行加密和解密的盐值
     */
    private static final String SALT = "zzl_wjs_xy_mySecretSalt123";

    // 布隆过滤器
    private RBloomFilter<String> userPhoneBloomFilter;

    /**
     * 布隆过滤器初始化
     */
    @PostConstruct
    public void initBloomFilter() {
        // 初始化Redisson布隆过滤器
        userPhoneBloomFilter = redissonClient.getBloomFilter("user:phone:bloom:filter");
        userPhoneBloomFilter.tryInit(1_000_000L, 0.0001);
        // 初始化数据(从索引表把所有的数据加载到布隆过滤器里面)
        List<String> allUserPhones = userPhoneMapper.selectAllUserPhonesFromIndex();
        allUserPhones.forEach(userPhoneBloomFilter::add);
    }

    /**
     * 用户登录
     *
     * @param phone
     * @return
     */
    @Override
    public UserLoginDTO login(String phone) {
        System.out.println("phone is " + phone);
        // 不能为空
        if (StringUtils.isEmpty(phone)) {
            return null;
        }

        // 判断用户是否已经注册过了
        UserPhoneDTO userPhoneDTO = queryByPhone(phone);
        if (userPhoneDTO != null) {
            // 注册过就生成token并返回
            Long userId = userPhoneDTO.getUserId();
            return UserLoginDTO.loginSuccess(userId, "");
        }

        // 未注册过就帮他注册一下
        return registerAndLogin(phone);
    }


    /**
     * 注册新手机号用户（注册 + 登录）
     *
     * @return
     */
    private UserLoginDTO registerAndLogin(String phone) {
        // 创建用户表记录并插入用户表
        Long userId = null;
        try {
            userId = idGenerateRpc.increaseSeqId(USER_ID.getCode());
            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(userId);
            userDTO.setNickName("星跃用户-" + userId);
            userDTO.setAvatar("https://assets.leetcode.cn/aliyun-lc-upload/users/mystifying-mestorfi5j/avatar_1724741118.png");
            userService.insertOne(userDTO);
            // 创建用户手机号表记录并插入
            UserPhonePO userPhonePO = new UserPhonePO();
            userPhonePO.setUserId(userId);
            userPhonePO.setPhone(encrypt(phone, SALT)); // 用户手机号进行加密
            userPhonePO.setStatus(CommonStatusEnum.VALID_STATUS.getCode());
            userPhoneMapper.insert(userPhonePO);
            // 将用户信息记录到 t_user_phone_index 表中
            userPhoneMapper.insertToIndexTable(encrypt(phone, SALT), userId);
            // 将当前手机号也添加到布隆过滤器里面
            userPhoneBloomFilter.add(encrypt(phone, SALT));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return UserLoginDTO.loginSuccess(userId, "");
    }


    /**
     * 根据手机号查询用户
     *
     * @param phone
     * @return
     */
    @Override
    public UserPhoneDTO queryByPhone(String phone) {
        try {
            // 判断是否为空
            if (StringUtils.isEmpty(phone)) {
                return null;
            }
            return queryByPhoneFromDB(phone);
//            // 先查询布隆过滤器是否有该数据
//            String encryptPhone = encrypt(phone, SALT);
//            if (!userPhoneBloomFilter.contains(encryptPhone)) {
//                return null;
//            }
//
//            // 说明这个用户是存在的，那么就判断一下是否在redis中
//            String key = userProviderCacheKeyBuilder.buildPhoneObjKey(phone);
//            // 查询redis
//            UserPhoneDTO userPhoneDTO = (UserPhoneDTO) redisTemplate.opsForValue().get(key);
//            if (userPhoneDTO != null) {
//                if (userPhoneDTO.getUserId() != null) return userPhoneDTO;
//            }
//
//            // 不在redis，需要查询数据库（拿到锁再查）
//            RLock lock = redissonClient.getLock(userProviderCacheKeyBuilder.buildUserPhoneLockKey(phone));
//            try {
//                boolean locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
//                if (!locked) {
//                    // 获取锁失败，可以再次检查缓存(有就返回，没有就是返回null)
//                    return (UserPhoneDTO) redisTemplate.opsForValue().get(key);
//                }
//
//                try {
//                    // 双重检查缓存（因为可能你是第二个拿到锁的，那么第一个线程已经将数据补偿到redis中了，区分是不是第一个线程来操作）
//                    userPhoneDTO = (UserPhoneDTO) redisTemplate.opsForValue().get(key);
//                    if (userPhoneDTO != null) {
//                        return userPhoneDTO;
//                    }
//
//                    // 说明是第一个拿到锁的，还是需要查询数据库
//                    userPhoneDTO = queryByPhoneFromDB(phone);
//                    if (userPhoneDTO != null) {
//                        // redis补偿
//                        redisTemplate.opsForValue().set(key, userPhoneDTO, 30, TimeUnit.MINUTES);
//                        return userPhoneDTO;
//                    }
//
//                    // 说明布隆过滤器没拦住或是没有正确加载，再来一个缓存null值（降级方案）
//                    redisTemplate.opsForValue().set(key, new UserPhoneDTO(), 5, TimeUnit.MINUTES);
//                    return null;
//                } finally {
//                    lock.unlock(); // 锁释放
//                }
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                throw new RuntimeException("获取锁被中断", e);
//            }  catch (Exception e) {
//                throw new RuntimeException("查询用户手机信息失败", e);
//            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 从数据库根据用户手机号查询UserPhone实例
     *
     * @param phone
     * @return
     */
    public UserPhoneDTO queryByPhoneFromDB(String phone) {
        UserPhonePO userPhonePO = null;
        try {
            phone = encrypt(phone, SALT);
            // 先查询到userId
            Long userId = userPhoneMapper.getUserIdByPhone(phone);
            if (userId == null) return null;
            LambdaQueryWrapper<UserPhonePO> userPhonePOLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userPhonePOLambdaQueryWrapper
                    .eq(UserPhonePO::getUserId, userId)
                    .eq(UserPhonePO::getStatus, CommonStatusEnum.VALID_STATUS.getCode())
                    .last("limit 1");
            userPhonePO = userPhoneMapper.selectOne(userPhonePOLambdaQueryWrapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ConvertBeanUtils.convert(userPhonePO, UserPhoneDTO.class);
    }



    /**
     * 根据用户Id批量的查询UserPhoneDTO
     *
     * @param userId
     * @return
     */
    @Override
    public List<UserPhoneDTO> queryByUserId(Long userId) {
        if (userId == null || userId < 10000) {
            return Collections.emptyList();
        }
        // 查询缓存中是否有数据
        String key = userProviderCacheKeyBuilder.buildPhoneListKey(userId);
        List<Object> UserPhoneDTOList = redisTemplate.opsForList().range(key, 0, -1);
        if (!CollectionUtils.isEmpty(UserPhoneDTOList)) {
            if (((UserPhoneDTO) (UserPhoneDTOList.get(0))).getUserId() != null) {
                return UserPhoneDTOList.stream().map(x -> (UserPhoneDTO) x).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
        // 没找到就需要查询数据库
        List<UserPhoneDTO> userPhoneDTOS = queryByUserIdFromDB(userId);
        if (!CollectionUtils.isEmpty(userPhoneDTOS)) {
            // 补偿
            redisTemplate.opsForList().leftPushAll(key, userPhoneDTOS.toArray());
            redisTemplate.expire(key, 30, TimeUnit.MINUTES);
            return userPhoneDTOS;
        }
        // 缓存穿透了
        redisTemplate.opsForList().leftPushAll(key, new UserPhoneDTO());
        redisTemplate.expire(key, 30, TimeUnit.SECONDS);
        return null;
    }

    /**
     * 从数据库根据用户Id批量的查询UserPhoneDTO
     *
     * @param userId
     * @return
     */
    private List<UserPhoneDTO> queryByUserIdFromDB(Long userId) {
        LambdaQueryWrapper<UserPhonePO> userPhonePOLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userPhonePOLambdaQueryWrapper
                .eq(UserPhonePO::getUserId, userId)
                .eq(UserPhonePO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        List<UserPhonePO> userPhonePOList = userPhoneMapper.selectList(userPhonePOLambdaQueryWrapper);
        return ConvertBeanUtils.convertList(userPhonePOList, UserPhoneDTO.class);
    }
    
    
}
