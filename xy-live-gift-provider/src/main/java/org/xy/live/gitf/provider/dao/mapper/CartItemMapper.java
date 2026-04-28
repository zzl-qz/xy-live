package org.xy.live.gitf.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.xy.live.gitf.provider.dao.po.CartItemPO;

import java.util.List;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItemPO> {

    // 修改或是添加
    @Insert("INSERT INTO t_cart_item(user_id,sku_id,qty,selected,room_id) " +
            "VALUES(#{userId},#{skuId},#{qty},#{selected},#{roomId}) " +
            "ON DUPLICATE KEY UPDATE qty=#{qty}, selected=#{selected}")
    void upsert(@Param("userId") Long userId,
                @Param("skuId") Long skuId,
                @Param("qty") Integer qty,
                @Param("selected") Boolean selected,
                @Param("roomId") Integer roomId);


    // 删除
    @Delete("DELETE FROM t_cart_item WHERE user_id=#{userId} AND sku_id=#{skuId}")
    void delete(@Param("userId") Long userId,
                @Param("skuId") Long skuId);

    // 清空购物车
    @Delete("DELETE FROM t_cart_item WHERE user_id=#{userId}")
    void clear(@Param("userId") Long userId);

    // 修改最后浏览时间
    @Update("UPDATE t_cart_item SET last_view_time = NOW() WHERE user_id = #{userId}")
    void touchLastView(Long userId);

    @Select("SELECT user_id " +
            "FROM t_cart_item " +
            "GROUP BY user_id " +
            "HAVING MAX(update_time) < NOW() - INTERVAL 7 DAY " +
            "   AND MAX(last_view_time) < NOW() - INTERVAL 7 DAY")
    List<Long> selectColdUserId();
}