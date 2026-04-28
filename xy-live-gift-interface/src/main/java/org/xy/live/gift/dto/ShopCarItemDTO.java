package org.xy.live.gift.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class ShopCarItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7247175817439564893L;

    private Integer qty; // 数量
    private boolean selected; // 是否勾选
    private Integer roomId; // 房间号Id
    private Double version; // 版本号

    public ShopCarItemDTO(Integer qty, boolean selected, Integer roomId) {
        this.qty = qty;
        this.selected = selected;
        this.roomId = roomId;
        this.version = 1.0; // 默认值
    }
}
