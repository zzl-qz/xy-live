package org.zzl.xy.live.api.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xy.live.bank.interfaces.constants.OrderStatusEnum;
import org.xy.live.bank.interfaces.constants.PaySourceEnum;
import org.xy.live.bank.interfaces.dto.PayOrderDTO;
import org.xy.live.bank.interfaces.dto.PayProductDTO;
import org.xy.live.bank.interfaces.dto.XyCurrencyAccountDTO;
import org.xy.live.bank.interfaces.rpc.IPayOrderRpc;
import org.xy.live.bank.interfaces.rpc.PayProductRpc;
import org.xy.live.bank.interfaces.rpc.XyCurrencyAccountRpc;
import org.xy.live.common.interfaces.vo.WebResponseVO;
import org.xy.live.web.starter.context.XyRequestContext;
import org.xy.live.web.starter.error.BizBaseErrorEnum;
import org.xy.live.web.starter.error.ErrorAssert;
import org.xy.live.web.starter.error.XyBaseError;
import org.zzl.xy.live.api.error.XyApiError;
import org.zzl.xy.live.api.service.IBankService;
import org.zzl.xy.live.api.vo.req.PayProductReqVO;
import org.zzl.xy.live.api.vo.resp.PayProductItemVO;
import org.zzl.xy.live.api.vo.resp.PayProductRespVO;
import org.zzl.xy.live.api.vo.resp.PayProductVO;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Service
public class BankServiceImpl implements IBankService {

    private final static Logger LOGGER = LoggerFactory.getLogger(BankServiceImpl.class);

    @DubboReference(timeout = 15000)
    private PayProductRpc payProductRpc;
    @DubboReference(timeout = 15000)
    private XyCurrencyAccountRpc xyCurrencyAccountRpc;

    @DubboReference(timeout = 15000)
    private IPayOrderRpc payOrderRpc;

    @Resource
    private RestTemplate restTemplate;



    @Override
    public PayProductVO products(Integer type) {
        // 查询产品列表
        List<PayProductDTO> payProductDTOS = payProductRpc.products(type);
        // 构建产品列表对象集合
        List<PayProductItemVO> payProductItemVOS = new ArrayList<>();
        for (PayProductDTO payProductDTO : payProductDTOS) {
            PayProductItemVO payProductItemVO = new PayProductItemVO();
            payProductItemVO.setId(payProductDTO.getId());
            payProductItemVO.setName(payProductDTO.getName());
            payProductItemVO.setCoinNum(JSON.parseObject(payProductDTO.getExtra()).getInteger("coin"));
            payProductItemVOS.add(payProductItemVO);
        }
        // 封装响应给前端的产品列表和当前用户钱包余额VO对象
        PayProductVO payProductVO = new PayProductVO();
        payProductVO.setCurrentBalance(xyCurrencyAccountRpc.getBalance(XyRequestContext.getUserId()));
        payProductVO.setPayProductItemVOList(payProductItemVOS);
        return payProductVO;
    }


    @Override
    public PayProductRespVO payProduct(PayProductReqVO payProductReqVO) {
        // 参数校验
        ErrorAssert.isTure(payProductReqVO != null && payProductReqVO.getProductId() != null && payProductReqVO.getPaySource() != null, BizBaseErrorEnum.PARAM_ERROR);
        ErrorAssert.isNotNull(PaySourceEnum.find(payProductReqVO.getPaySource()), BizBaseErrorEnum.PARAM_ERROR);
        // 查询payProductDTO
        PayProductDTO payProductDTO = payProductRpc.getByProductId(payProductReqVO.getProductId());
        LOGGER.info("[payProduct] getByProductId res is {}", payProductDTO);
        ErrorAssert.isNotNull(payProductDTO, BizBaseErrorEnum.PARAM_ERROR);

        // 生成一条订单（待支付状态）
        PayOrderDTO payOrderDTO = new PayOrderDTO();
        payOrderDTO.setProductId(payProductReqVO.getProductId());
        payOrderDTO.setUserId(XyRequestContext.getUserId());
        payOrderDTO.setPayTime(new Date());
        payOrderDTO.setSource(payProductReqVO.getPaySource());
        payOrderDTO.setPayChannel(payProductReqVO.getPayChannel());
        LOGGER.info("[payProduct] userId is {}", XyRequestContext.getUserId());
        String orderId = payOrderRpc.insertOne(payOrderDTO);
        LOGGER.info("[payProduct] orderId is {}", orderId);

        // 正确的流程应该是点击到具体的商品页面的时候就会生成一条订单，然后等我们点击去支付的时候就会执行下面的逻辑去执行支付的流程
        // 模拟点击 去支付 按钮，更新订单状态为 支付中
        payOrderRpc.updateOrderStatus(orderId, OrderStatusEnum.PAYING.getCode());
        PayProductRespVO payProductRespVO = new PayProductRespVO();
        payProductRespVO.setOrderId(orderId);

        // TODO 这里应该是支付成功后，由第三方支付所做的事情，因为我们是模拟支付，所以我们直接发起支付成功后的回调请求：
        com.alibaba.fastjson2.JSONObject jsonObject = new JSONObject();
        jsonObject.put("orderId", orderId);
        jsonObject.put("userId", XyRequestContext.getUserId());
        jsonObject.put("bizCode", 10001);
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("param", jsonObject.toJSONString());
        // 使用RestTemplate进行HTTP的发送
        ResponseEntity<String> resultEntity =
                restTemplate.postForEntity("http://localhost:8201/live/bank/payNotify/wxNotify?param={param}",
                        null, String.class, paramMap);

        // TODO 这里拿到 业务code 之后一般就是发送一条特定的mq消息，让特定的服务去进行消费
        System.out.println(resultEntity.getBody());
        return payProductRespVO;
    }

    @Override
    public XyCurrencyAccountDTO getByUserId() {
        XyCurrencyAccountDTO currencyAccountDTO = xyCurrencyAccountRpc.getByUserId(XyRequestContext.getUserId());
        ErrorAssert.isNotNull(currencyAccountDTO, XyApiError.NO_USER_EXIT);
        return currencyAccountDTO;
    }
}