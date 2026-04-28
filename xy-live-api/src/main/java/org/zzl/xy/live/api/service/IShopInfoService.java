package org.zzl.xy.live.api.service;

import org.zzl.xy.live.api.vo.req.ShopCarReqVO;
import org.zzl.xy.live.api.vo.req.SkuInfoReqVO;
import org.zzl.xy.live.api.vo.resp.*;

import java.util.List;

public interface IShopInfoService {

    /**
     * 根据anchorId查询商品列表
     */
    List<SkuInfoVO> queryByAnchorId(Long anchorId);

    /**
     * 根据skuId查询商品详情信息
     */
    SkuDetailInfoVO detail(SkuInfoReqVO skuInfoReqVO);

    /**
     * 添加购物车
     */
    Boolean addCar(ShopCarReqVO reqVO);

    /**
     * 移除购物车
     */
    Boolean removeFromCar(ShopCarReqVO reqVO);

    /**
     * 清空购物车
     */
    Boolean clearShopCar(ShopCarReqVO reqVO);

    /**
     * 修改购物车中某个商品的数量
     */
    Boolean changeCarItemNum(ShopCarReqVO reqVO, Integer changeType);

    /**
     * 查看购物车信息
     */
    ShopCarRespVO getCarInfo(ShopCarReqVO reqVO);


    /**
     * 进行预下单操作
     */
    boolean prepareOrder(PrepareOrderVO prepareOrderVO);

    /**
     * 准备库存到Redis
     */
    boolean prepareStock(Long anchorId);

    /**
     * 订单支付(直播间内支付)
     * @param prepareOrderVO
     * @return
     */
    boolean payNow(PrepareOrderVO prepareOrderVO);

    /**
     * 订单支付（全局购物车支付）
     * @param orderId
     * @return
     */
    boolean payNowByOrderId(Long orderId);

    /**
     * 根据用户Id和直播间Id查询订单信息
     * @param userId
     * @param roomId
     * @return
     */
    List<ShopOrderVO> getOrderInfoByUserIdAndRoomId(Long userId, Integer roomId);

    /**
     * 根据用户Id查询订单信息
     * @param userId
     * @return
     */
    List<ShopOrderVO> getOrderInfoByUserId(Long userId);

    /**
     * 修改购物车中的商品的选中状态
     * @param shopCarReqVO
     * @return
     */
    Boolean updateSelectedFlag(ShopCarReqVO shopCarReqVO);

    /**
     * 清空购物车(v2版本)
     * @param reqVO
     * @return
     */
    Boolean clearCar_v2(ShopCarReqVO reqVO);

    /**
     * 查询购物车(V2版本)
     * @param reqVO
     * @return
     */
    ShopCarRespVO getCarInfo_v2(ShopCarReqVO reqVO);

    /**
     * 从购物车中移除信息（v2，也是只操作内存版本）
     * @param reqVO
     * @return
     */
    Boolean removeFromCar_v2(ShopCarReqVO reqVO);

    /**
     * 往购物车中添加商品（v2版本就是只会保存到redis）
     * @param reqVO
     * @return
     */
    Boolean addCar_v2(ShopCarReqVO reqVO);

    /**
     * 修改购物车水平数量（v2）
     * @param reqVO
     * @param changeType
     * @return
     */
    Boolean changeCarItemNum_v2(ShopCarReqVO reqVO, Integer changeType);
}