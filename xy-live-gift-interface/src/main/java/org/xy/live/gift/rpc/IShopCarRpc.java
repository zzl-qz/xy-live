package org.xy.live.gift.rpc;

import org.xy.live.gift.dto.ShopCarReqDTO;
import org.xy.live.gift.dto.ShopCarRespDTO;

public interface IShopCarRpc {

    /**
     * 添加商品到购物车中
     */
    Boolean addCar(ShopCarReqDTO shopCarReqDTO);

    /**
     * 移除购物车
     */
    Boolean removeFromCar(ShopCarReqDTO shopCarReqDTO);

    /**
     * 清空购物车
     */
    Boolean clearShopCar(ShopCarReqDTO shopCarReqDTO);

    /**
     * 修改购物车中某个商品的数量
     */
    Boolean changeCarItemNum(ShopCarReqDTO shopCarReqDTO, Integer changeType);

    /**
     * 查看购物车信息
     */
    ShopCarRespDTO getCarInfo(ShopCarReqDTO shopCarReqDTO);

    /**
     * 修改购物车中的商品的选中状态
     */
    Boolean updateSelectedFlag(ShopCarReqDTO shopCarReqDTO);

    /**
     * 清空购物车(v2版本)
     * @param reqVO
     * @return
     */
    Boolean clearShopCar_v2(ShopCarReqDTO reqVO);

    /**
     * 查询购物车(V2版本)
     * @param reqVO
     * @return
     */
    ShopCarRespDTO getCarInfo_v2(ShopCarReqDTO reqVO);

    /**
     * 从购物车中移除信息（v2，也是只操作内存版本）
     * @param reqVO
     * @return
     */
    Boolean removeFromCar_v2(ShopCarReqDTO reqVO);

    /**
     * 往购物车中添加商品（v2版本就是只会保存到redis）
     * @param reqVO
     * @return
     */
    Boolean addCar_v2(ShopCarReqDTO reqVO);

    /**
     * 修改购物车中某个商品的数量(v2)
     */
    Boolean changeCarItemNum_v2(ShopCarReqDTO shopCarReqDTO, Integer changeType);

}