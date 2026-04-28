package org.xy.live.id.generate.interfaces;

public interface IdGenerateRpc {

    /**
     * 根据本地步长度来生成唯一id(区间性递增)
     *
     * @return
     */
    Long increaseSeqId(Integer code);

    /**
     * 生成的是非连续性id
     *
     * @param code
     * @return
     */
    Long increaseUnSeqId(Integer code);
}
