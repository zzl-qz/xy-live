package org.xy.live.msg.provider.dao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xy.live.msg.provider.dao.po.SmsPO;

@Mapper
public interface SmsMapper extends BaseMapper<SmsPO> {
}