package org.xy.live.gitf.provider.bo;

import lombok.Data;

@Data
public class CartChangeEventBO {
    private Long userId;
    private Long skuId;
    private Integer qty;
    private Boolean selected;
    private Integer roomId;
}