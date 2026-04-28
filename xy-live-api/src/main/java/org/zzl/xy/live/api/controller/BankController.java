package org.zzl.xy.live.api.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xy.live.bank.interfaces.dto.XyCurrencyAccountDTO;
import org.xy.live.common.interfaces.vo.WebResponseVO;
import org.xy.live.web.starter.error.BizBaseErrorEnum;
import org.xy.live.web.starter.error.ErrorAssert;
import org.zzl.xy.live.api.service.IBankService;
import org.zzl.xy.live.api.vo.req.PayProductReqVO;
import org.zzl.xy.live.api.vo.resp.PayProductRespVO;
import org.zzl.xy.live.api.vo.resp.PayProductVO;

@RestController
@RequestMapping("/bank")
public class BankController {
    
    @Resource
    private IBankService bankService;

    /**
     * 将商品列表集合返回给前端
     * @param type
     * @return
     */
    @PostMapping("/products")
    public WebResponseVO<PayProductVO> products(Integer type) {
        ErrorAssert.isNotNull(type, BizBaseErrorEnum.PARAM_ERROR);
        return WebResponseVO.success(bankService.products(type)); 
    }


    // 1.申请调用第三方支付接口（签名-》支付宝/微信）（生成一条支付中状态的订单）
    // 2.生成一个（特定的支付页）二维码（输入账户密码，支付）（第三方平台完成）
    // 3.发送回调请求-》业务方
    // 要求（可以接收不同平台的回调数据）
    // 可以根据业务标识去回调不同的业务服务（自定义参数组成中，塞入一个业务code,根据业务code去回调不同的业务服务）
    @PostMapping("/payProduct")
    public WebResponseVO<PayProductRespVO> payProduct(@RequestBody PayProductReqVO payProductReqVO) {
        return WebResponseVO.success(bankService.payProduct(payProductReqVO));
    }


    // 根据用户id查询当前用户的账户情况
    @PostMapping("/getByUserId")
    public WebResponseVO<XyCurrencyAccountDTO> getByUserId() {
        return WebResponseVO.success(bankService.getByUserId());
    }

}