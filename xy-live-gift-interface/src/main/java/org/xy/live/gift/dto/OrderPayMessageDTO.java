package org.xy.live.gift.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderPayMessageDTO implements Serializable {

    private Long id;
    private Integer orderNo;
    private Long userId;
    private Integer payAmount;
    private Integer messageStatus;
    private String errorMsg;

}