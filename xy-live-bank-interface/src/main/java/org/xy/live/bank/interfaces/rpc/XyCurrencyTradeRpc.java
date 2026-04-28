package org.xy.live.bank.interfaces.rpc;

import org.xy.live.bank.interfaces.dto.XyCurrencyTradeDTO;

/**
 * @Author: Daylight
 * @Date: 2026-01-29 21:17
 */
public interface XyCurrencyTradeRpc {
    XyCurrencyTradeDTO getByExtraCol(int code, String extraCol);

}
