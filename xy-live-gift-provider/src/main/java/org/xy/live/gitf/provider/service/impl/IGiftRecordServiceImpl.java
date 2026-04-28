package org.xy.live.gitf.provider.service.impl;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.gift.dto.GiftRecordDTO;
import org.xy.live.gitf.provider.dao.mapper.GiftRecordMapper;
import org.xy.live.gitf.provider.dao.po.GiftRecordPO;
import org.xy.live.gitf.provider.service.IGiftRecordService;

@Service
public class IGiftRecordServiceImpl implements IGiftRecordService {

    @Resource
    private GiftRecordMapper giftRecordMapper;
    @Override
    public void insertOne(GiftRecordDTO giftRecordDTO) {
        giftRecordMapper.insert(ConvertBeanUtils.convert(giftRecordDTO, GiftRecordPO.class));
    }
}
