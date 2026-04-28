package org.xy.live.bank.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xy.live.bank.provider.dao.po.PayOrderPO;
import org.xy.live.bank.provider.dao.po.PayProductPO;

@Mapper
public interface PayOrderMapper extends BaseMapper<PayOrderPO> {
}