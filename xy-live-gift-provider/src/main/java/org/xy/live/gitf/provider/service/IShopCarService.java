package org.xy.live.gitf.provider.service;

import org.xy.live.gift.dto.ShopCarReqDTO;
import org.xy.live.gift.dto.ShopCarRespDTO;

public interface IShopCarService {

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
     * 修改选中状态
     * @param shopCarReqDTO
     * @return
     */
    Boolean updateSelectedFlag(ShopCarReqDTO shopCarReqDTO);

    /**
     * 对冷用户购物车移除缓存
     * @return
     */
    void removeColdUserCartInCache();


    /**
     * 清空购物车（v2版本）
     * @param reqVO
     * @return
     */
    Boolean clearShopCar_v2(ShopCarReqDTO reqVO);

    /**
     * 获取购物车信息（v2版本）
     * @param reqVO
     * @return
     */
    ShopCarRespDTO getCarInfo_v2(ShopCarReqDTO reqVO);

    /**
     * 将商品移除出购物车（v2版本）
     * @param reqVO
     * @return
     */
    Boolean removeFromCar_v2(ShopCarReqDTO reqVO);

    /**
     * 将商品添加到购物车（v2版本）
     * @param reqVO
     * @return
     */
    Boolean addCar_v2(ShopCarReqDTO reqVO);


    /**
     * 修改购物车商品数量（v2版本）
     * @param reqVO
     * @param changeType
     * @return
     */
    Boolean changeCarItemNum_v2(ShopCarReqDTO reqVO, Integer changeType);
}