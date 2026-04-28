package org.xy.live.gift.dto;


import lombok.Data;

@Data
public class ReceiveRedPacketHandlerMq {

    private String code;
    private Integer price;
    private Long userId;

}
