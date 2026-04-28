package org.xy.live.id.generate.provider.service;

public interface IdGenerateService {

    /**
     * 生成有序id
     *
     * @param code
     * @return
     */
    Long getSeqId(Integer code);

    /**
     * 生成无序id
     *
     * @param code
     * @return
     */
    Long getUnSeqId(Integer code);
}
