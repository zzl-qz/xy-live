package org.xy.live.gitf.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.xy.live.gitf.provider.dao.po.SkuOrderInfoPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ISkuOrderInfoMapper extends BaseMapper<SkuOrderInfoPO> {


    /**
     * 修改订单状态
     * @param id 订单Id
     * @param status 最终状态
     * @param oldStatus 原先状态
     * @return
     */
    @Update("UPDATE t_sku_order_info SET status = #{status} WHERE id = #{id} AND status = #{oldStatus}")
    int updateOrderStatus(@Param("id")          Long id,
                          @Param("status")      Integer status,
                          @Param("oldStatus")   Integer oldStatus
    );


}