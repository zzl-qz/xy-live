package org.xy.live.gift.rpc;

import org.xy.live.gift.dto.GiftRecordDTO;

public interface IGiftRecordRpc {

    /**
     * 插入一条送礼记录
     */
    void insertOne(GiftRecordDTO giftRecordDTO);

}
