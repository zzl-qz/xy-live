package org.xy.live.bank.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.xy.live.bank.provider.dao.mapper.PayTopicMapper;
import org.xy.live.bank.provider.dao.po.PayTopicPO;
import org.xy.live.bank.provider.service.IPayTopicService;
import org.xy.live.common.interfaces.enums.CommonStatusEnum;

@Service
public class PayTopicServiceImpl implements IPayTopicService {

    @Resource
    private PayTopicMapper payTopicMapper;
    @Override
    public PayTopicPO getByCode(Integer code) {
        LambdaQueryWrapper<PayTopicPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PayTopicPO::getBizCode, code);
        queryWrapper.eq(PayTopicPO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        queryWrapper.last("limit 1");
        PayTopicPO payTopicPO = payTopicMapper.selectOne(queryWrapper);
        return payTopicPO;
    }
}
