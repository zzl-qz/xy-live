package org.xy.live.bank.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.xy.live.bank.provider.dao.po.XyCurrencyAccountPO;

@Mapper
public interface XyCurrencyAccountMapper extends BaseMapper<XyCurrencyAccountPO> {
    
    @Update("update t_xy_currency_account set current_balance = current_balance + #{num} where user_id = #{userId}")
    void incr(@Param("userId") Long userId, @Param("num") int num);

    @Update("update t_xy_currency_account set current_balance = current_balance - #{num} where user_id = #{userId}")
    void decr(@Param("userId") Long userId, @Param("num") int num);

    @Select("select current_balance from t_xy_currency_account where user_id = #{userId} and status = 1")
    Integer queryBalance(Long userId);
}