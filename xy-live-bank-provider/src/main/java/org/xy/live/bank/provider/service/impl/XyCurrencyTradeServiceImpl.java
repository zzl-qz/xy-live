package org.xy.live.bank.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.xy.live.bank.interfaces.dto.XyCurrencyTradeDTO;
import org.xy.live.common.interfaces.enums.TradeTypeEnum;
import org.xy.live.bank.provider.dao.mapper.XyCurrencyTradeMapper;
import org.xy.live.bank.provider.dao.po.XyCurrencyTradePO;
import org.xy.live.bank.provider.service.XyCurrencyTradeService;
import org.xy.live.common.interfaces.enums.CommonStatusEnum;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;

@Service
public class XyCurrencyTradeServiceImpl implements XyCurrencyTradeService {

    @Resource
    private XyCurrencyTradeMapper xyCurrencyTradeMapper;
    @Override
    public boolean insertOne(long userId, int num, int type, String extraCol) {
        // 判断一下code是否是有效的
        boolean contain = TradeTypeEnum.isContain(type);
        // 类型不匹配
        if (!contain) {
            return false;
        }
        XyCurrencyTradePO xyCurrencyTradePO = new XyCurrencyTradePO();
        xyCurrencyTradePO.setUserId(userId);
        xyCurrencyTradePO.setNum(num);
        xyCurrencyTradePO.setType(type);
        xyCurrencyTradePO.setExtraCol(extraCol);
        xyCurrencyTradePO.setStatus(CommonStatusEnum.VALID_STATUS.getCode());
        xyCurrencyTradeMapper.insert(xyCurrencyTradePO);
        return true;
    }

    public XyCurrencyTradeDTO getByExtraCol(int code, String extraCol) {
        // 是订单支付相关的，传递过来的是orderId
        if (code == TradeTypeEnum.PAY_ORDER.getCode()) {
            LambdaQueryWrapper<XyCurrencyTradePO> qw = new LambdaQueryWrapper<>();
            qw.eq(XyCurrencyTradePO::getExtraCol, extraCol);
            qw.eq(XyCurrencyTradePO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
            qw.last("limit 1");
            XyCurrencyTradePO xyCurrencyTradePO = xyCurrencyTradeMapper.selectOne(qw);
            return ConvertBeanUtils.convert(xyCurrencyTradePO, XyCurrencyTradeDTO.class);
        }
        return null;
    }

}
