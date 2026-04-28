package org.xy.live.gitf.provider.service;

import java.util.List;

public interface IAnchorShopInfoService {

    /**
     * 根据主播id查询出主播带货商品的id
     * @param anchorId
     * @return
     */

    List<Long>  querySkuIdsByAnchorId(Long anchorId);

    /**
     * 查询所有有效的主播id列表
     */
    List<Long> queryAllValidAnchorId();
}
