package org.xy.live.living.provider.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.xy.live.common.interfaces.dto.PageWrapper;
import org.xy.live.common.interfaces.enums.CommonStatusEnum;
import org.xy.live.common.interfaces.topic.GiftProviderTopicNames;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.framework.redis.starter.key.LivingProviderCacheKeyBuilder;
import org.xy.live.im.constants.AppIdEnum;
import org.xy.live.im.core.server.interfaces.dto.ImOfflineDTO;
import org.xy.live.im.core.server.interfaces.dto.ImOnlineDTO;
import org.xy.live.im.dto.ImMsgBody;
import org.xy.live.im.router.constants.ImMsgBizCodeEnum;
import org.xy.live.im.router.interfaces.ImRouterRpc;
import org.xy.live.living.interfaces.dto.LivingPkRespDTO;
import org.xy.live.living.interfaces.dto.LivingRoomReqDTO;
import org.xy.live.living.interfaces.dto.LivingRoomRespDTO;
import org.xy.live.living.provider.dao.mapper.LivingRoomMapper;
import org.xy.live.living.provider.dao.mapper.LivingRoomRecordMapper;
import org.xy.live.living.provider.dao.po.LivingRoomPO;
import org.xy.live.living.provider.dao.po.LivingRoomRecordPO;
import org.xy.live.living.provider.service.ILivingRoomService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class LivingRoomServiceImpl implements ILivingRoomService {


    @Resource
    private LivingRoomMapper livingRoomMapper;
    @Resource
    private LivingRoomRecordMapper livingRoomRecordMapper;
    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private LivingProviderCacheKeyBuilder cacheKeyBuilder;


    private final String listKey = "living_online_list";

    @DubboReference
    private ImRouterRpc imRouterRpc;



    @Resource
    private MQProducer mqProducer;


    private Logger LOGGER = LoggerFactory.getLogger(LivingRoomServiceImpl.class);

    @Override
    public Integer startLivingRoom(LivingRoomReqDTO livingRoomReqDTO) {

        // 先查询一下是不是上次直播还没有关闭
        LivingRoomRespDTO livingRoomRespDTO = this.queryByAnchorId(livingRoomReqDTO.getAnchorId());
        if (livingRoomRespDTO != null) {
            throw new RuntimeException("您当前正在直播，请勿重复开播");
        }

        // 往直播表中插入一条记录
        LivingRoomPO livingRoomPO = BeanUtil.copyProperties(livingRoomReqDTO, LivingRoomPO.class);
        livingRoomPO.setStatus(CommonStatusEnum.VALID_STATUS.getCode());
        livingRoomPO.setStartTime(new Date());
        livingRoomMapper.insert(livingRoomPO);
        String cacheKey = cacheKeyBuilder.buildLivingRoomObj(livingRoomPO.getId());
        // 防止之前有空值缓存，这里做移除操作
        redisTemplate.delete(cacheKey);

        // 往直播间列表插入一条记录(这里是为了方便直接一次性查出所有的直播间id)
        long now = Instant.now().getEpochSecond();
        redisTemplate.opsForZSet().add(listKey, livingRoomPO.getId().toString(), now);

        // 发送mq进行异步商品库存加载
        Message message = new Message();
        message.setTopic(GiftProviderTopicNames.START_LIVING_ROOM);
        message.setBody(JSON.toJSONBytes(livingRoomReqDTO.getAnchorId()));
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("发送mq进行异步商品库存加载，{}", sendResult);
        } catch (Exception e) {
            LOGGER.error("发送mq进行异步商品库存加载失败，", e);
        }
        return livingRoomPO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeLiving(LivingRoomReqDTO livingRoomReqDTO) {
        // 判断直播间是否存在以及是否是主播本人要关闭直播间
        LivingRoomPO livingRoomPO = livingRoomMapper.selectById(livingRoomReqDTO.getRoomId());
        if (livingRoomPO == null) {
            return false;
        }
        if (!livingRoomReqDTO.getAnchorId().equals(livingRoomPO.getAnchorId())) {
            return false;
        }
        // 往历史直播记录表中插入一条数据
        LivingRoomRecordPO livingRoomRecordPO = BeanUtil.copyProperties(livingRoomPO, LivingRoomRecordPO.class);
        livingRoomRecordPO.setEndTime(new Date());
        livingRoomRecordPO.setStatus(CommonStatusEnum.INVALID_STATUS.getCode());
        livingRoomRecordMapper.insert(livingRoomRecordPO);

        // 将直播表中的记录删除掉
        livingRoomMapper.deleteById(livingRoomPO.getId());

        // 移除掉redis中的直播间缓存
        String cacheKey = cacheKeyBuilder.buildLivingRoomObj(livingRoomReqDTO.getRoomId());
        redisTemplate.delete(cacheKey);

        // 删除在线直播间id列表
        redisTemplate.opsForZSet().remove(listKey, livingRoomReqDTO.getRoomId().toString());

        // todo 直播过程中可能有其他的缓存信息也需要及时的删除掉(比如商品信息以及购物车信息之类的)

        return true;
    }

    @Override
    public LivingRoomRespDTO queryByRoomId(Integer roomId) {
        // 查询redis
        String cacheKey = cacheKeyBuilder.buildLivingRoomObj(roomId);
        LivingRoomRespDTO queryResult = (LivingRoomRespDTO) redisTemplate.opsForValue().get(cacheKey);
        if (queryResult != null) {
            // 空值缓存
            if (queryResult.getId() == null) {
                return null;
            }
            return queryResult;
        }
        // 查询数据库
        LambdaQueryWrapper<LivingRoomPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LivingRoomPO::getId, roomId);
        queryWrapper.eq(LivingRoomPO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        queryWrapper.last("limit 1");
        LivingRoomPO livingRoomPO = livingRoomMapper.selectOne(queryWrapper);
        if (livingRoomPO == null) {
            // 防止缓存穿透
            redisTemplate.opsForValue().set(cacheKey, new LivingRoomRespDTO(), 1L, TimeUnit.MINUTES);
            return null;
        }
        // 补偿操作
        queryResult = BeanUtil.copyProperties(livingRoomPO, LivingRoomRespDTO.class);
        redisTemplate.opsForValue().set(cacheKey, queryResult, 30, TimeUnit.MINUTES);
        return queryResult;
    }


    /**
     * 分页查询直播间列表记录
     * @param livingRoomReqDTO
     * @return
     */
    @Override
    public PageWrapper<LivingRoomRespDTO> list(LivingRoomReqDTO livingRoomReqDTO) {
        // 因为直播平台同时在线直播人数不算太多，属于读多写少场景，所以将其缓存进Redis进行提速
        String cacheKey = cacheKeyBuilder.buildLivingRoomList(livingRoomReqDTO.getType());
        int page = livingRoomReqDTO.getPage();
        int pageSize = livingRoomReqDTO.getPageSize();
        // 获取总记录数
        Long total = redisTemplate.opsForList().size(cacheKey);
        // 获取执行范围内的数据
        List<Object> resultList = redisTemplate.opsForList().range(cacheKey, (long) (page - 1) * pageSize, (long) page * pageSize);
        PageWrapper<LivingRoomRespDTO> pageWrapper = new PageWrapper<>();
        if (CollectionUtils.isEmpty(resultList)) {
            pageWrapper.setList(Collections.emptyList());
            pageWrapper.setHasNext(false);
        } else {
            // 将集合的类型进行强转
            pageWrapper.setList(ConvertBeanUtils.convertList(resultList, LivingRoomRespDTO.class));
            // 有没有下一页是通过当前是否到最后的长度了
            pageWrapper.setHasNext((long) page * pageSize < total);
        }
        return pageWrapper;
    }

    @Override
    public List<LivingRoomRespDTO> listAllLivingRoomFromDB(Integer type) {
        LambdaQueryWrapper<LivingRoomPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LivingRoomPO::getType, type);
        queryWrapper.eq(LivingRoomPO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        //按照时间倒序展示
        queryWrapper.orderByDesc(LivingRoomPO::getStartTime);
        //为性能做保证，只查询1000个
        queryWrapper.last("limit 10000");
        return ConvertBeanUtils.convertList(livingRoomMapper.selectList(queryWrapper), LivingRoomRespDTO.class);
    }


    @Override
    public void userOfflineHandler(ImOfflineDTO imOfflineDTO) {
        // 其实就是将set集合中的记录删除掉
        LOGGER.info("offline handler,imOfflineDTO is {}", imOfflineDTO);
        Long userId = imOfflineDTO.getUserId();
        Integer roomId = imOfflineDTO.getRoomId();
        Integer appId = imOfflineDTO.getAppId();
        String cacheKey = cacheKeyBuilder.buildLivingRoomUserSet(roomId, appId);
        redisTemplate.opsForSet().remove(cacheKey, userId);

        // 监听挑战者下线信息
        LivingRoomReqDTO livingRoomReqDTO = new LivingRoomReqDTO();
        livingRoomReqDTO.setPkObjId(userId);
        livingRoomReqDTO.setRoomId(roomId);
        this.offlinePk(livingRoomReqDTO);
    }

    @Override
    public void userOnlineHandler(ImOnlineDTO imOnlineDTO) {
        // 其实就是往set集合中塞入一条记录
        LOGGER.info("online handler,imOnlineDTO is {}", imOnlineDTO);
        Long userId = imOnlineDTO.getUserId();
        Integer roomId = imOnlineDTO.getRoomId();
        Integer appId = imOnlineDTO.getAppId();
        String cacheKey = cacheKeyBuilder.buildLivingRoomUserSet(roomId, appId);
        //set集合中
        redisTemplate.opsForSet().add(cacheKey, userId);
        // 一般一场直播最多不会超过半天，所以我们默认就是只绑定半天的时间，超过的话，用户重新进入就行了
        redisTemplate.expire(cacheKey, 12, TimeUnit.HOURS);
    }

    @Override
    public List<Long> queryUserIdsByRoomId(LivingRoomReqDTO livingRoomReqDTO) {
        Integer roomId = livingRoomReqDTO.getRoomId();
        Integer appId = livingRoomReqDTO.getAppId();
        String cacheKey = cacheKeyBuilder.buildLivingRoomUserSet(roomId, appId);
        //使用scan分批查询数据，否则set元素太多容易造成redis和网络阻塞（scan会自动分成多次请求去执行）
        Cursor<Object> cursor = redisTemplate.opsForSet().scan(cacheKey, ScanOptions.scanOptions().match("*").count(100).build());
        List<Long> userIdList = new ArrayList<>();
        // 遍历scan获取到的数据，有点像迭代器
        while (cursor.hasNext()) {
            userIdList.add(Long.valueOf(cursor.next().toString()));
        }
        return userIdList;
    }

    @Override
    public Long queryOnlinePkUserId(Integer roomId) {
        String cacheKey = cacheKeyBuilder.buildLivingOnlinePk(roomId);
        Object userId = redisTemplate.opsForValue().get(cacheKey);
        return userId != null ? (long) (int) userId : null;
    }

    @Override
    public boolean offlinePk(LivingRoomReqDTO livingRoomReqDTO) {
        // 将直播间信息查询出来
        Integer roomId = livingRoomReqDTO.getRoomId();
        Long userId = livingRoomReqDTO.getPkObjId(); // 调用方传递进来的用户id
        Long pkObjId = this.queryOnlinePkUserId(roomId); // 当前挑战者Id
        LivingRoomRespDTO currentLivingRoom = this.queryByRoomId(roomId);
        if (currentLivingRoom == null) {
            return false;
        }

        String cacheKey = cacheKeyBuilder.buildLivingOnlinePk(livingRoomReqDTO.getRoomId());
        // 将redis中存储的用户信息删除掉，确保其他的用户可以上线进行pk
        // 判断是否是本人在进行操作
        if (!userId.equals(pkObjId)) {
            LOGGER.info("删除失败，非本人操作");
            return false;
        }
        // 删除pk进度条缓存
        redisTemplate.delete("xy-live-gift-provider:living_pk_key:" + roomId);
        // 删除PK直播间pkObjId缓存
        return Boolean.TRUE.equals(redisTemplate.delete(cacheKey));
    }

    @Override
    public LivingPkRespDTO onlinePk(LivingRoomReqDTO livingRoomReqDTO) {
        // 将直播间信息查询出来
        LivingRoomRespDTO currentLivingRoom = this.queryByRoomId(livingRoomReqDTO.getRoomId());
        LivingPkRespDTO respDTO = new LivingPkRespDTO();
        respDTO.setOnlineStatus(false);
        // 判断是不是主播本人
        if (currentLivingRoom.getAnchorId().equals(livingRoomReqDTO.getPkObjId())) {
            respDTO.setMsg("主播不可以连线参与pk");
            return respDTO;
        }

        // 判断当前是否有人在连线中,如果没有的话就将当前pk连线人的Id塞到redis中
        String cacheKey = cacheKeyBuilder.buildLivingOnlinePk(currentLivingRoom.getId());
        // 这里不能只用set，而需要用setnx的原因就是如果是set的话会出现覆盖的情况，比如前一个用户刚连接上，结果第二个用户点击了连线，导致前一个用户的连线状态被覆盖
        Boolean tryOnline = redisTemplate.opsForValue().setIfAbsent(cacheKey, livingRoomReqDTO.getPkObjId(), 30, TimeUnit.HOURS);
        LOGGER.info("[onlinePk] tryOnline is {}", tryOnline);
        // 连线成功
        if (tryOnline) {
            respDTO.setMsg("连线成功");
            respDTO.setOnlineStatus(true);
            // 批量的将连线信息推送给直播间的每一个用户
            List<Long> userIdList = this.queryUserIdsByRoomId(livingRoomReqDTO);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("pkObjId", livingRoomReqDTO.getPkObjId());
            jsonObject.put("pkObjAvatar", "https://i-avatar.csdnimg.cn/685d3b76e6124582ae3afc870e08b8c1_2301_80275666.jpg!1");
            this.batchSendImMsg(userIdList, ImMsgBizCodeEnum.LIVING_ROOM_PK_ONLINE.getCode(), jsonObject);
        } else {
            respDTO.setMsg("目前有人在线，请稍后再试");
        }
        return respDTO;
    }

    @Override
    public LivingRoomRespDTO queryByAnchorId(Long anchorId) {
        LambdaQueryWrapper<LivingRoomPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LivingRoomPO::getAnchorId, anchorId);
        queryWrapper.eq(LivingRoomPO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        queryWrapper.last("limit 1");
        LivingRoomPO livingRoomPO = livingRoomMapper.selectOne(queryWrapper);
        return livingRoomPO != null ? ConvertBeanUtils.convert(livingRoomPO, LivingRoomRespDTO.class) : null;
    }


    /**
     * 批量的将连线信息推送给直播间的每一个用户
     *
     * @param userIdList
     * @param bizCode
     * @param jsonObject
     */
    private void batchSendImMsg(List<Long> userIdList, int bizCode, JSONObject jsonObject) {
        List<ImMsgBody> imMsgBodies = userIdList.stream().map(userId -> {
            ImMsgBody imMsgBody = new ImMsgBody();
            imMsgBody.setAppId(AppIdEnum.XY_LIVE_BIZ.getCode());
            imMsgBody.setBizCode(bizCode);
            imMsgBody.setUserId(userId);
            imMsgBody.setMsgId(UUID.randomUUID().toString());
            imMsgBody.setData(jsonObject.toJSONString());
            return imMsgBody;
        }).collect(Collectors.toList());
        imRouterRpc.batchSendMsg(imMsgBodies);
    }

}









