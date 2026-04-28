package org.xy.live.gitf.provider.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Daylight
 * @Date: 2026-01-29 16:03
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentSuccessHandleBO {

    Long userId;
    Integer roomId;
    Long orderId;

}
