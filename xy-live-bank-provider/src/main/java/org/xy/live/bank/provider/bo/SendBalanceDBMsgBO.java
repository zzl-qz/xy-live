package org.xy.live.bank.provider.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.xy.live.common.interfaces.enums.TradeTypeEnum;

/**
 * @Author: Daylight
 * @Date: 2026-02-03 19:45
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendBalanceDBMsgBO {
    private Long userId;
    private int num;
    private TradeTypeEnum tradeTypeEnum;
    private String extraCol;
}
