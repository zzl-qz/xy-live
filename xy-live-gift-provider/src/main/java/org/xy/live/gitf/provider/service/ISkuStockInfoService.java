package org.xy.live.gitf.provider.service;

import org.xy.live.gitf.provider.bo.RollBackStockBO;
import org.xy.live.gitf.provider.dao.po.SkuStockInfoPO;

import java.util.List;

public interface ISkuStockInfoService {

    /**
     * 根据stuId更新库存
     */
    boolean updateStockNum(Long skuId, Integer stockNum);

    /**
     * 根据stuId扣减库存值 v1版本
     */
    boolean decrStockNumBySkuId(Long skuId, Integer num);

    /**
     * 使用lua脚本扣减缓存的库存值 v2版本
     */
    boolean decrStockNumBySkuIdByLua(Long skuId, Integer num);

    /**
     * 使用lua脚本增加缓存的库存值
     * @param skuId
     * @param num
     * @return
     */
    boolean incrStockNumBySkuIdByLua(Long skuId, Integer num);

    /**
     * 根据skuId查询库存值
     */
    SkuStockInfoPO queryBySkuId(Long skuId);

    /**
     * 根据stuIdList批量查询数据
     */
    List<SkuStockInfoPO> queryBySkuIds(List<Long> skuIdList);


    /**
     * 库存回滚
     */
    void stockRollBackHandler(RollBackStockBO rollBackStockBO);


    /**
     * 修改库存（带乐观锁）
     */
    boolean updateStockNumByTypeAndSkuId(Long skuId, Integer num, Integer type, String msgId);

    /**
     * 发送库存同步信息
     */
    boolean SendMsgToSyncDBStock(long skuId, int num, int type, int cnt, String msgId);
}
