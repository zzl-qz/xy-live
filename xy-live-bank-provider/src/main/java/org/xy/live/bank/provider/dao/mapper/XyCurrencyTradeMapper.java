package org.xy.live.bank.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.xy.live.bank.provider.dao.po.XyCurrencyAccountPO;
import org.xy.live.bank.provider.dao.po.XyCurrencyTradePO;

@Mapper
public interface XyCurrencyTradeMapper extends BaseMapper<XyCurrencyTradePO> {

}