package org.xy.live.bank.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xy.live.bank.interfaces.dto.XyOrderPayTradeDTO;
import org.xy.live.bank.provider.dao.mapper.XyOrderPayTradeMapper;
import org.xy.live.bank.provider.dao.po.XyOrderPayTradePO;
import org.xy.live.bank.provider.service.XyOrderPayTradeService;
import org.xy.live.common.interfaces.enums.CommonStatusEnum;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;

/**
 * @Author: Daylight
 * @Date: 2026-01-12 21:04
 */

@Service
public class XyOrderPayTradeServiceImpl implements XyOrderPayTradeService {


    @Resource
    private XyOrderPayTradeMapper xyOrderPayTradeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertOne(long userId, int num, String orderId, String remark) {
        XyOrderPayTradePO xyOrderPayTradePO = new XyOrderPayTradePO();
        xyOrderPayTradePO.setOrderId(orderId);
        xyOrderPayTradePO.setUserId(userId);
        xyOrderPayTradePO.setNum(num);
        xyOrderPayTradePO.setStatus(CommonStatusEnum.VALID_STATUS.getCode());
        xyOrderPayTradePO.setRemark(remark);
        xyOrderPayTradeMapper.insert(xyOrderPayTradePO);
        return true;
    }

    @Override
    public XyOrderPayTradeDTO queryByOrderId(String orderId) {
        LambdaQueryWrapper<XyOrderPayTradePO> qw = new LambdaQueryWrapper<>();
        qw.eq(XyOrderPayTradePO::getOrderId, orderId);
        qw.eq(XyOrderPayTradePO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        qw.last("limit 1");
        XyOrderPayTradePO xyOrderPayTradePO = xyOrderPayTradeMapper.selectOne(qw);
        return ConvertBeanUtils.convert(xyOrderPayTradePO, XyOrderPayTradeDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByOrderId(String orderId) {
        LambdaQueryWrapper<XyOrderPayTradePO> qw = new LambdaQueryWrapper<>();
        qw.eq(XyOrderPayTradePO::getOrderId, orderId);
        qw.eq(XyOrderPayTradePO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        qw.last("limit 1");
        xyOrderPayTradeMapper.delete(qw);
        return true;
    }
}
