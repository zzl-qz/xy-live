package org.zzl.xy.live.api.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xy.live.common.interfaces.dto.SendGiftMq;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.gift.dto.GiftConfigDTO;
import org.xy.live.gift.rpc.IGiftConfigRpc;
import org.xy.live.web.starter.context.XyRequestContext;
import org.xy.live.web.starter.error.ErrorAssert;
import org.zzl.xy.live.api.service.IGiftService;
import org.zzl.xy.live.api.vo.req.GiftReqVO;
import org.zzl.xy.live.api.vo.resp.GiftConfigVO;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.xy.live.common.interfaces.topic.GiftProviderTopicNames.SEND_GIFT;
import static org.zzl.xy.live.api.error.XyApiError.GIFT_CONFIG_ERROR;
import static org.zzl.xy.live.api.error.XyApiError.NOT_SEND_TO_YOURSELF;

@Service
public class GiftServiceImpl implements IGiftService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GiftServiceImpl.class);

    @DubboReference(timeout = 15000)
    private IGiftConfigRpc giftConfigRpc;

    @Resource
    private MQProducer mqProducer;

    // 本地缓存
    private Cache<Integer, GiftConfigDTO> giftConfigDTOCache =
            Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(90, TimeUnit.SECONDS).build();

    @Override
    public List<GiftConfigVO> listGift() {
        List<GiftConfigDTO> giftConfigDTOList = giftConfigRpc.queryGiftList();
        return ConvertBeanUtils.convertList(giftConfigDTOList, GiftConfigVO.class);
    }

    @Override
    public boolean send(GiftReqVO giftReqVO) {

        // 查询本地缓存，根据giftId进行查询，如果获取不到的话就会调用后面的那个方法，通过远程rpc调用得到，并将调用得到的值塞到giftConfigDTOCache中
        int giftId = giftReqVO.getGiftId();
        GiftConfigDTO giftConfigDTO = giftConfigDTOCache.get(giftId, id -> giftConfigRpc.getByGiftId(giftId));

        // 判断礼物配置信息是否是null
        ErrorAssert.isNotNull(giftConfigDTO, GIFT_CONFIG_ERROR);
        // 判断是否是自己给自己送礼物
        ErrorAssert.isTure(!giftReqVO.getSenderUserId().equals(giftReqVO.getReceiverId()), NOT_SEND_TO_YOURSELF);

        // 发送异步消息，去进行异步消费（削峰填谷）
        SendGiftMq sendGiftMq = new SendGiftMq();
        sendGiftMq.setUserId(XyRequestContext.getUserId());
        sendGiftMq.setGiftId(giftId);
        sendGiftMq.setPrice(giftConfigDTO.getPrice());
        sendGiftMq.setReceiverId(giftReqVO.getReceiverId());
        sendGiftMq.setRoomId(giftReqVO.getRoomId());
        sendGiftMq.setUrl(giftConfigDTO.getSvgaUrl());
        sendGiftMq.setUuid(UUID.randomUUID().toString()); // 唯一标识，避免重复消费
        sendGiftMq.setType(giftReqVO.getType());
        Message message = new Message();
        message.setTopic(SEND_GIFT);
        message.setBody(JSON.toJSONString(sendGiftMq).getBytes());

        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("[gift-send] send result is {}", sendResult);
        } catch (Exception e) {
            LOGGER.info("[gift-send] error is", e);
            return false;
        }
        return true;
    }
}