package org.xy.live.gitf.provider.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueAveragely;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.client.RestTemplate;
import org.xy.live.bank.interfaces.dto.AccountTradeReqDTO;
import org.xy.live.bank.interfaces.dto.AccountTradeRespDTO;
import org.xy.live.bank.interfaces.rpc.XyCurrencyAccountRpc;
import org.xy.live.common.interfaces.dto.SendGiftMq;
import org.xy.live.common.interfaces.topic.GiftProviderTopicNames;
import org.xy.live.framework.mq.starter.properties.RocketMQConsumerProperties;
import org.xy.live.framework.redis.starter.key.GiftProviderCacheKeyBuilder;
import org.xy.live.gift.constants.SendGiftTypeEnum;
import org.xy.live.im.constants.AppIdEnum;
import org.xy.live.im.dto.ImMsgBody;
import org.xy.live.im.router.constants.ImMsgBizCodeEnum;
import org.xy.live.im.router.interfaces.ImRouterRpc;
import org.xy.live.living.interfaces.dto.LivingRoomReqDTO;
import org.xy.live.living.interfaces.dto.LivingRoomRespDTO;
import org.xy.live.living.interfaces.rpc.ILivingRoomRpc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.xy.live.common.interfaces.topic.ShopCarProviderTopicNames.CLEAR_CAR_MSG_TOPIC;

/**
 * 发送礼物异步消息消费
 */
@Configuration
public class SendGiftConsumer implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendGiftConsumer.class);

    private static final Long PK_INIT_NUM = 500L;
    private static final Long PK_MAX_NUM = 1000L; // 进度条最大值（如果大于等于这个值说明pk结束）
    private static final Long PK_MIN_NUM = 0L; // 进度条最小值

    /**
     * 在 Redis 中判断某个 key 是否存在，
     * 如果不存在则初始化它为一个初始值（这里设置是 500）并设置过期时间；
     * 如果存在，则判断它的当前值是否在指定的范围内，若在范围内就增加一个步长并返回新值，否则返回当前值。
     */
    private final String LUA_SCRIPT =
            "if (redis.call('exists', KEYS[1])) == 1 then " +
                    " local currentNum=redis.call('get',KEYS[1]) " +
                    " if (tonumber(currentNum)<=tonumber(ARGV[2]) and tonumber(currentNum)>=tonumber(ARGV[3])) then " +
                    " return redis.call('incrby',KEYS[1],tonumber(ARGV[4])) " +
                    " else return currentNum end " +
                    "else " +
                    "redis.call('set', KEYS[1], tonumber(ARGV[1])) " +
                    "redis.call('EXPIRE', KEYS[1], 3600 * 12) " +
                    "return ARGV[1] end";

    @Resource
    private RocketMQConsumerProperties rocketMQConsumerProperties;
    @Resource(name = "customRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private GiftProviderCacheKeyBuilder cacheKeyBuilder;
    @DubboReference
    private XyCurrencyAccountRpc xyCurrencyAccountRpc;
    @DubboReference
    private ILivingRoomRpc livingRoomRpc;
    @DubboReference
    private ImRouterRpc routerRpc;

    @Override
    public void afterPropertiesSet() throws Exception {

        AclClientRPCHook rpcHook = new AclClientRPCHook(
                new SessionCredentials(rocketMQConsumerProperties.getAccessKey(), rocketMQConsumerProperties.getSecretKey()));

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                rocketMQConsumerProperties.getGroupName()  +  "_" + SendGiftConsumer.class.getSimpleName(),
                rpcHook,
                new AllocateMessageQueueAveragely()
        );

        consumer.setVipChannelEnabled(false);
        consumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv()); // NameServer地址
        consumer.setConsumeMessageBatchMaxSize(10); // 批量消费最大条数
        consumer.setAccessChannel(AccessChannel.valueOf("CLOUD"));
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET); // 从第一条开始消费
        consumer.subscribe(GiftProviderTopicNames.SEND_GIFT, "*"); // 订阅缓存删除主题


//        DefaultMQPushConsumer mqPushConsumer = new DefaultMQPushConsumer();
//        //老版本中会开启，新版本的mq不需要使用到
//        mqPushConsumer.setVipChannelEnabled(false);
//        mqPushConsumer.setNamesrvAddr(rocketMQConsumerProperties.getNameSrv());
//        mqPushConsumer.setConsumerGroup(rocketMQConsumerProperties.getGroupName() + "_" + SendGiftConsumer.class.getSimpleName());
//        mqPushConsumer.setConsumeMessageBatchMaxSize(10);
//        mqPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
//        //监听礼物缓存数据更新的行为
//        mqPushConsumer.subscribe(GiftProviderTopicNames.SEND_GIFT, "*");


        consumer.setMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                SendGiftMq sendGiftMq = JSON.parseObject(new String(msg.getBody()), SendGiftMq.class);
                LOGGER.info("执行到消费逻辑了{}", sendGiftMq);
                String mqConsumerKey = cacheKeyBuilder.buildGiftConsumeKey(sendGiftMq.getUuid());
                boolean lockStatus = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(mqConsumerKey, -1, 5, TimeUnit.MINUTES));
                if (!lockStatus) {
                    //代表曾经消费过
                    continue;
                }

                // 记录流水信息
                AccountTradeReqDTO tradeReqDTO = new AccountTradeReqDTO();
                tradeReqDTO.setUserId(sendGiftMq.getUserId());
                tradeReqDTO.setNum(sendGiftMq.getPrice());
                AccountTradeRespDTO tradeRespDTO = xyCurrencyAccountRpc.consumeForSendGift(tradeReqDTO);

                //如果余额扣减成功
                Integer sendGiftType = sendGiftMq.getType();
                JSONObject jsonObject = new JSONObject();

                // 改成全直播间可见
                // 送礼成功
                if (tradeRespDTO.isSuccess()) {
                    LOGGER.info("送礼成功，开始推送信息");
                    Long receiverId = sendGiftMq.getReceiverId();
                    // 普通送礼
                    if (SendGiftTypeEnum.DEFAULT_SEND_GIFT.getCode().equals(sendGiftType)) {
                        LOGGER.info("普通送礼");
                        // 触发礼物特效推送功能
                        jsonObject.put("url", sendGiftMq.getUrl());
                        LivingRoomReqDTO reqDTO = new LivingRoomReqDTO();
                        reqDTO.setAppId(AppIdEnum.XY_LIVE_BIZ.getCode());
                        reqDTO.setRoomId(sendGiftMq.getRoomId());
                        // 获取直播间所有用户的Id
                        List<Long> userIdList = livingRoomRpc.queryUserIdsByRoomId(reqDTO);
                        // 批量的去推送给直播间的所有用户
                        this.batchSendImMsg(userIdList, ImMsgBizCodeEnum.LIVING_ROOM_SEND_GIFT_SUCCESS, jsonObject);
                    } else if (SendGiftTypeEnum.PK_SEND_GIFT.getCode().equals(sendGiftType)) {
                        LOGGER.info("开始执行pk送礼推送逻辑");
                        // pk送礼
                        this.pkImMsgSend(jsonObject, sendGiftMq, receiverId);
                    }
                } else {
                    // 送礼失败，利用im将发送失败的消息告知用户
                    jsonObject.put("msg", tradeRespDTO.getMsg());
                    this.sendImMsgSingleton(sendGiftMq.getUserId(), ImMsgBizCodeEnum.LIVING_ROOM_SEND_GIFT_FAIL.getCode(), jsonObject);
                }
//                LOGGER.info("[SendGiftConsumer] msg is {}", msg);
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();
        LOGGER.info("mq消费者启动成功,namesrv is {}", rocketMQConsumerProperties.getNameSrv());
    }


    /**
     * 单独发送im消息
     *
     * @param userId
     * @param bizCode
     * @param jsonObject
     */
    private void sendImMsgSingleton(Long userId, int bizCode, JSONObject jsonObject) {
        ImMsgBody imMsgBody = new ImMsgBody();
        imMsgBody.setAppId(AppIdEnum.XY_LIVE_BIZ.getCode());
        imMsgBody.setBizCode(bizCode);
        imMsgBody.setUserId(userId);
        imMsgBody.setData(jsonObject.toJSONString());
        routerRpc.sendMsg(imMsgBody);
    }

    /**
     * pk直播间送礼信息处理（礼物特效，送礼进度条）
     * @param jsonObject
     * @param sendGiftMq
     * @param receiverId
     */
    private void pkImMsgSend(JSONObject jsonObject, SendGiftMq sendGiftMq, Long receiverId) {
        //pk类型的送礼 要通知什么给直播间的用户
        //url 礼物特效全直播间可见
        //进度条全直播间可见
        // 1000,进度条长度一共是1000，每个礼物对于进度条的影响就是一个数值（500（A）：500（B），550：450）
        // 直播pk进度是不是以roomId为维度，string，送礼（A）incr，送礼给（B）就是decr。

        // 判断pk是否已经结束
        Integer roomId = sendGiftMq.getRoomId();
        String isOverCacheKey = cacheKeyBuilder.buildLivingPkIsOver(roomId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(isOverCacheKey))) {
            return;
        }

        // 查询当前房间和 PK 参与者信息
        // 查询直播间信息
        LivingRoomRespDTO respDTO = livingRoomRpc.queryByRoomId(roomId);
        // 挑战者id
        Long pkObjId = livingRoomRpc.queryOnlinePkUserId(roomId);
        if (pkObjId == null || respDTO == null || respDTO.getAnchorId() == null) {
            return;
        }
        // 房主id
        Long pkUserId = respDTO.getAnchorId();

        Long pkNum = 0L; // 用来记录当前进度
        String pkNumKey = cacheKeyBuilder.buildLivingPkKey(roomId); // 这个key是用来记录当前直播间pk进度的
        // 执行lua脚本，大概逻辑就是：
        //   如果 key 不存在 → 初始化一个 PK 计数器（比如初始值 PK_INIT_NUM）
        //   如果 key 存在 → 判断当前值是否在某个范围内，如果在，就增加或减少一个步长（moveStep）
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(LUA_SCRIPT);
        redisScript.setResultType(Long.class);

        // 当前时间戳，主要是用来标记这条送礼信息的时间的，避免因为出现说网络问题，导致进度不一致的情况
        Long sendGiftSeqNum = System.currentTimeMillis();


        // 判断礼物是发给挑战者还是房主
        //   如果是房主的话就是对进度条做增操作
        //   如果是挑战者的话就是减操作
        // 礼物接收方是房主
        if (pkUserId.equals(receiverId)) {
            // 计算进度条增长补偿（价格/10）
            Integer moveStep = sendGiftMq.getPrice() / 10;
            // 获取当前进度条
            pkNum = this.redisTemplate.execute(redisScript, Collections.singletonList(pkNumKey), PK_INIT_NUM, PK_MAX_NUM, PK_MIN_NUM, moveStep);
            // 如果当前进度条已经大于1000.说明主播获胜
            if (PK_MAX_NUM <= pkNum) {
                jsonObject.put("winnerId", pkUserId);
            }
        }
        // 礼物接收方是挑战者
        else if (pkObjId.equals(receiverId)) {
            // 计算进度条增长补偿（价格/10），并且挑战者需要转成负数
            Integer moveStep = sendGiftMq.getPrice() / 10 * -1;
            pkNum = this.redisTemplate.execute(redisScript, Collections.singletonList(pkNumKey), PK_INIT_NUM, PK_MAX_NUM, PK_MIN_NUM, moveStep);
            // 进入条小于0，说明挑战者获胜
            if (PK_MIN_NUM >= pkNum) {
                this.redisTemplate.opsForValue().set(cacheKeyBuilder.buildLivingPkIsOver(roomId),-1);
                jsonObject.put("winnerId", pkObjId);
            }
        }

        // 构建要发送IM的消息（这个消息是发送给前端的）
        jsonObject.put("receiverId", sendGiftMq.getReceiverId());
        jsonObject.put("sendGiftSeqNum", sendGiftSeqNum); // 时间戳，前端拿到之后可以根据这个字段来进行排序渲染，就不会导致出现进度条错误的情况
        jsonObject.put("pkNum", pkNum); // 进度条
        jsonObject.put("url", sendGiftMq.getUrl()); // 这个是用来触发礼物特效的关键
        LivingRoomReqDTO livingRoomReqDTO = new LivingRoomReqDTO();
        livingRoomReqDTO.setRoomId(roomId);
        livingRoomReqDTO.setAppId(AppIdEnum.XY_LIVE_BIZ.getCode());
        // 查询直播间所有用户id
        List<Long> userIdList = livingRoomRpc.queryUserIdsByRoomId(livingRoomReqDTO);
        LOGGER.info("[pkImMsgSend] 直播间用户有userIdList is {}", userIdList);
        // 将进度和礼物特效推送给直播间的所有用户
        this.batchSendImMsg(userIdList, ImMsgBizCodeEnum.LIVING_ROOM_PK_SEND_GIFT_SUCCESS, jsonObject);
    }

    /**
     * 批量发送im消息，就是用来将某一条消息批量的发送给直播间的所有人（用户）
     *
     * @param userIdList
     * @param imMsgBizCodeEnum
     * @param jsonObject
     */
    private void batchSendImMsg(List<Long> userIdList, ImMsgBizCodeEnum imMsgBizCodeEnum, JSONObject jsonObject) {
        LOGGER.info("[batchSendImMsg] 开始推送信息");
        String msgId = UUID.randomUUID().toString();
        // 遍历每一个用户Id，并构建出对应的对象
        List<ImMsgBody> imMsgBodies = userIdList.stream().map(userId -> {
            ImMsgBody imMsgBody = new ImMsgBody();
            imMsgBody.setAppId(AppIdEnum.XY_LIVE_BIZ.getCode());
            imMsgBody.setBizCode(imMsgBizCodeEnum.getCode());
            imMsgBody.setUserId(userId);
            imMsgBody.setMsgId(msgId);
            imMsgBody.setData(jsonObject.toJSONString());
            return imMsgBody;
        }).collect(Collectors.toList());
        routerRpc.batchSendMsg(imMsgBodies);
    }
}
