package org.xy.live.bank.interfaces.dto;

import lombok.Data;

/**
 * @Author: Daylight
 * @Date: 2026-01-12 11:15
 */

@Data
public class XyOrderPayTradeDTO {

    private Long id;
    private String orderId; // 订单Id（业务id）
    private Long userId;
    private Integer num;
    private Integer status;
    private String remark; // 拓展字段

}
