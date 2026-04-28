package org.xy.live.gitf.provider.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RollBackStockBO {
    
    private Long userId;
    private Long orderId;
    private Integer count; // 重发次数，如果超过最大次数就不能继续重试了
}