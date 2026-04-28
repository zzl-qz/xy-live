package org.xy.live.bank.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class XyCurrencyTradeDTO {

    private Long id;
    private Long userId;
    private Integer type;
    private Integer status;
    private Integer num;
    private String extraCol;
}