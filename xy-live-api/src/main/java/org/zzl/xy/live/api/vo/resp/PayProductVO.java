package org.zzl.xy.live.api.vo.resp;

import lombok.Data;

import java.util.List;

/**
 * 前端展示的产品列表
 */
@Data
public class PayProductVO {

    /**
     * 当前余额
     */
    private Integer currentBalance;
    /**
     * 一系列付费产品
     */
    private List<PayProductItemVO> payProductItemVOList;
}