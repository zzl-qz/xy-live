package org.xy.live.gitf.provider.bo;

import lombok.Data;

/**
 * @Author: Daylight
 * @Date: 2026-01-27 14:59
 * 发送mq消息去同步db库存
 */

@Data
public class SendMsgToSyncDBStockBO {
    private Long skuId;
    private Integer num;
    private Integer type; // 0 incr   1 decr
    private Integer cnt;
    private String msgId; // 消息的唯一code，避免重复消费
}
