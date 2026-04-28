package org.xy.live.bank.interfaces.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class PayOrderDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7789861579943422191L;
    private Long id;
    private String orderId;
    private Integer productId;
    private Integer bizCode; // 标识这条信息是哪个业务要进行处理的，主要是在mp处理的时候，不同的服务能知道这条消息是不是给自己的
    private Long userId;
    private Integer source;
    private Integer payChannel;
    private Integer status;
    private Date payTime;
}