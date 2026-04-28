package org.xy.live.im.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class BankMsgBody implements Serializable {
    @Serial
    private static final long serialVersionUID = -7657602083071950966L;
    private Long id;
    private String orderId;
    private Integer productId;
    private Long userId;
    private Integer source;
    private Integer payChannel;
    private Integer status;
    private Date payTime;
    private Date createTime;
    private Date updateTime;
}
