package org.xy.live.gitf.provider.service;

import org.xy.live.gift.dto.GiftRecordDTO;

public interface IGiftRecordService {

    /**
     * 插入一条送礼记录
     */
    void insertOne(GiftRecordDTO giftRecordDTO);
}