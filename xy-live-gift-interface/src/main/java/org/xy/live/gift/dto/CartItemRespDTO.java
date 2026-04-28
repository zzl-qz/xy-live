package org.xy.live.gift.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CartItemRespDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -1394363192115983898L;

    private Long id;
    private Long userId;
    private Long skuId;
    private Integer qty;
    private Integer selected;
    private Long roomId;
    // private Integer size;
    private Integer status;

}
