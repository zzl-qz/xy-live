package org.xy.live.bank.provider.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xy.live.bank.interfaces.constants.OrderStatusEnum;
import org.xy.live.bank.interfaces.constants.PayProductTypeEnum;
import org.xy.live.common.interfaces.enums.TradeTypeEnum;
import org.xy.live.bank.interfaces.dto.PayOrderDTO;
import org.xy.live.bank.interfaces.dto.PayProductDTO;
import org.xy.live.bank.provider.dao.mapper.PayOrderMapper;
import org.xy.live.bank.provider.dao.po.PayOrderPO;
import org.xy.live.bank.provider.dao.po.PayTopicPO;
import org.xy.live.bank.provider.service.IPayTopicService;
import org.xy.live.bank.provider.service.PayOrderService;
import org.xy.live.bank.provider.service.PayProductService;
import org.xy.live.bank.provider.service.XyCurrencyAccountService;
import org.xy.live.common.interfaces.enums.CommonStatusEnum;

import java.util.UUID;

@Service
public class PayOrderServiceImpl implements PayOrderService {

    @Resource
    private PayOrderMapper payOrderMapper;

    @Resource
    private XyCurrencyAccountService xyCurrencyAccountService;

    @Resource
    private IPayTopicService payTopicService;

    @Resource
    private MQProducer mqProducer;

    @Resource
    private PayProductService payProductService;

    private static final Logger LOGGER = LoggerFactory.getLogger(PayOrderServiceImpl.class);

    @Override
    public PayOrderPO queryByOrderId(String orderId) {
        // 这里直接查询数据库，因为并不是每秒都有几千的人来充值的，不需要上缓存
        LambdaQueryWrapper<PayOrderPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PayOrderPO::getOrderId, orderId);
        queryWrapper.eq(PayOrderPO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        queryWrapper.last("limit 1");
        return payOrderMapper.selectOne(queryWrapper);
    }

    @Override
    public String insertOne(PayOrderPO payOrderPO) {
        // todo 随机生成一条订单Id（这里后续要直接调用来生成）
        String orderId = UUID.randomUUID().toString();
        payOrderPO.setOrderId(orderId);
        payOrderMapper.insert(payOrderPO);
        return payOrderPO.getOrderId();
    }

    @Override
    public boolean updateOrderStatus(Long id, Integer status) {
        PayOrderPO payOrderPO = new PayOrderPO();
        payOrderPO.setId(id);
        payOrderPO.setStatus(status);
        return payOrderMapper.updateById(payOrderPO) > 0;
    }

    @Override
    public boolean updateOrderStatus(String orderId, Integer status) {
        PayOrderPO payOrderPO = new PayOrderPO();
        payOrderPO.setStatus(status);
        LambdaUpdateWrapper<PayOrderPO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PayOrderPO::getOrderId, orderId);
        return payOrderMapper.update(payOrderPO, updateWrapper) > 0;
    }

    @Override
    public boolean payNotify(PayOrderDTO payOrderDTO) {
        // 参数校验
        PayOrderPO payOrderPO = this.queryByOrderId(payOrderDTO.getOrderId());
        if (payOrderPO == null) {
            LOGGER.error("[PayOrderServiceImpl] payOrderPO is null, create a payOrderPO, userId is {}", payOrderDTO.getUserId());
            return false;
        }
        PayTopicPO payTopicPO = payTopicService.getByCode(payOrderDTO.getBizCode());
        if (payTopicPO == null || StringUtils.isEmpty(payTopicPO.getTopic())) {
            LOGGER.error("[PayOrderServiceImpl] error payTopicPO, payTopicPO is {}", payTopicPO);
            return false;
        }

        // 更新订单状态并增加用户余额
        this.payNotifyHandler(payOrderPO);

        // 假设 需要去发送一条支付成功的信息给到用户 -》调用msg
        // 假设 需要修改用户的VIP经验值 -》 account服务之类的
        // 所以这里最好的方式就是直接发送mq，让订阅的服务自己去处理
        Message message = new Message();
        message.setTopic(payTopicPO.getTopic());
        message.setBody(JSON.toJSONBytes(payOrderPO));
        try {
            SendResult sendResult = mqProducer.send(message);
            LOGGER.info("[PayOrderServiceImpl] payNotify: send success, orderId is {}, sendResult is{}", payOrderDTO.getOrderId(), sendResult);
        } catch (Exception e) {
            LOGGER.error("[PayOrderServiceImpl] payNotify: send failed, orderId is {}", payOrderDTO.getOrderId());
        }
        return true;
    }


    /**
     * 在bank层处理一些操作：
     * 如 判断充值商品类型，去做对应的商品记录（如：购买虚拟币，进行余额增加，和流水记录）
     */
    private void payNotifyHandler(PayOrderPO payOrderPO) {
        // 更新订单状态为已支付
        this.updateOrderStatus(payOrderPO.getOrderId(), OrderStatusEnum.PAYED.getCode());
        // 查询充值流水，并对用户余额进行增加操作
        Integer productId = payOrderPO.getProductId();
        PayProductDTO payProductDTO = payProductService.getByProductId(productId);
        if (payProductDTO != null && payProductDTO.getType().equals(PayProductTypeEnum.XY_COIN.getCode())) {
            // 类型是充值虚拟币业务：
            Long userId = payOrderPO.getUserId();
            // {"coin":300,"url":"www.tttt.com"}
            JSONObject jsonObject = JSON.parseObject(payProductDTO.getExtra());
            Integer coinNum = jsonObject.getInteger("coin");
            xyCurrencyAccountService.incr(userId, coinNum, TradeTypeEnum.LIVING_RECHARGE, "");
        }
    }

}
