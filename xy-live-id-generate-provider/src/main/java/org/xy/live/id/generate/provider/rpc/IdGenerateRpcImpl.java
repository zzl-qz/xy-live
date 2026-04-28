package org.xy.live.id.generate.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.id.generate.interfaces.IdGenerateRpc;
import org.xy.live.id.generate.provider.service.IdGenerateService;

/**
 * @Author Daylight
 * @Description
 */
@DubboService
public class IdGenerateRpcImpl implements IdGenerateRpc {

    @Resource
    private IdGenerateService idGenerateService;

    @Override
    public Long increaseSeqId(Integer code) {
        Long seqId = idGenerateService.getSeqId(code);
        return seqId;
    }

    @Override
    public Long increaseUnSeqId(Integer code) {
        Long unSeqId = idGenerateService.getUnSeqId(code);
        return unSeqId;
    }
}
