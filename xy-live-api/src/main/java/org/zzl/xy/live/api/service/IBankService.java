package org.zzl.xy.live.api.service;

import org.xy.live.bank.interfaces.dto.XyCurrencyAccountDTO;
import org.xy.live.common.interfaces.vo.WebResponseVO;
import org.zzl.xy.live.api.vo.req.PayProductReqVO;
import org.zzl.xy.live.api.vo.resp.PayProductRespVO;
import org.zzl.xy.live.api.vo.resp.PayProductVO;

public interface IBankService {

    /**
     * 查询相关产品信息
     */
    PayProductVO products(Integer type);

    /**
     * 发起支付
     * @param payProductReqVO
     * @return
     */
    PayProductRespVO payProduct(PayProductReqVO payProductReqVO);

    /**
     * 根据用户id查询当前用户的账户情况
     * @return
     */
    XyCurrencyAccountDTO getByUserId();

}