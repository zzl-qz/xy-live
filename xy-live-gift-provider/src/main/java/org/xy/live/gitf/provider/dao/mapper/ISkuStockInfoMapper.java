package org.xy.live.gitf.provider.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.xy.live.gitf.provider.dao.po.SkuStockInfoPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ISkuStockInfoMapper extends BaseMapper<SkuStockInfoPO> {

    /**
     * 对库存进行扣减操作
     * @param skuId
     * @param num
     * @return
     */
    @Update("update t_sku_stock_info set stock_num = stock_num - #{num} where sku_id = #{skuId} and stock_num >= #{num}")
    boolean decrStockNumBySkuId(@Param("skuId") Long skuId, @Param("num") Integer num);


    // 带乐观锁的扣减库存
    @Update("update t_sku_stock_info set stock_num = stock_num - #{num}, version = version + 1 " +
            "where sku_id = #{skuId} and stock_num >= #{num} and version = #{version}")
    int decrStockNum(Long skuId, Integer num, Integer version);

    // 带乐观锁的增加库存
    @Update("update t_sku_stock_info set stock_num = stock_num + #{num}, version = version + 1 " +
            "where sku_id = #{skuId} and version = #{version}")
    int incrStockNum(Long skuId, Integer num, Integer version);
}