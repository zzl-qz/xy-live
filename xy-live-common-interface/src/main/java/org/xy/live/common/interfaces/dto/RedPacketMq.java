package org.xy.live.common.interfaces.dto;

import lombok.Data;


/**
 * 用户抢到红包之后就发送一条mq消息去消费
 */
@Data
public class RedPacketMq {

    private Long userId;
    private Integer price;
}