package org.zzl.xy.live.api.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xy.live.common.interfaces.vo.WebResponseVO;
import org.xy.live.web.starter.context.XyRequestContext;
import org.xy.live.web.starter.limit.RequestLimit;
import org.zzl.xy.live.api.service.IShopInfoService;
import org.zzl.xy.live.api.vo.req.ShopCarReqVO;
import org.zzl.xy.live.api.vo.req.SkuInfoReqVO;
import org.zzl.xy.live.api.vo.resp.*;

import java.util.List;

@RestController
@RequestMapping("/shop")
public class ShopInfoController {
    
    @Resource
    private IShopInfoService shopInfoService;

    /**
     * 根据主播Id查询直播间商品列表信息
     * @param anchorId
     * @return
     */
    @PostMapping("/listSkuInfo")
    public WebResponseVO<List<SkuInfoVO>> listSkuInfo(Long anchorId) {
        return WebResponseVO.success(shopInfoService.queryByAnchorId(anchorId));
    }

    /**
     * 查询某一个具体的商品
     * @param reqVO
     * @return
     */
    @PostMapping("/detail")
    public WebResponseVO<SkuDetailInfoVO> detail(@RequestBody SkuInfoReqVO reqVO) {
        return WebResponseVO.success(shopInfoService.detail(reqVO));
    }

    /**
     * 添加到购物车，主要是把商品添加到购物车中
     * @param reqVO
     * @return
     */
    @PostMapping("/addCar")
    public WebResponseVO<Boolean> addCar(@RequestBody ShopCarReqVO reqVO) {
        return WebResponseVO.success(shopInfoService.addCar(reqVO));
    }




    /**
     * 从购物车移除
     * @param reqVO
     * @return
     */
    @PostMapping("/removeFromCar")
    public WebResponseVO<Boolean> removeFromCar(@RequestBody ShopCarReqVO reqVO) {
        return WebResponseVO.success(shopInfoService.removeFromCar(reqVO));
    }


    /**
     * 查询购物车
     * @param reqVO
     * @return
     */
    @PostMapping("/getCarInfo")
    public WebResponseVO<ShopCarRespVO> getCarInfo(@RequestBody ShopCarReqVO reqVO) {
        return WebResponseVO.success(shopInfoService.getCarInfo(reqVO));
    }


    /**
     * 清空购物车
     * @param reqVO
     * @return
     */
    @PostMapping("/clearCar")
    public WebResponseVO<Boolean> clearCar(@RequestBody ShopCarReqVO reqVO) {
        return WebResponseVO.success(shopInfoService.clearShopCar(reqVO));
    }



    /**
     * 修改购物车某一个商品数量
     * @param reqVO
     * @param changeType
     * @return
     */
    @PostMapping("/changeCarSkuNum")
    public WebResponseVO<Boolean> changeCarSkuNum(@RequestBody ShopCarReqVO reqVO, Integer changeType) {
        return WebResponseVO.success(shopInfoService.changeCarItemNum(reqVO, changeType));
    }

    /**
     * 预下单操作
     * @param prepareOrderVO
     * @return
     */
    @PostMapping("/preparerOrder")
    @RequestLimit(limit = 1, second = 1, msg = "") // 限制一下一秒内只能请求一次，避免出现抖动情况
    public WebResponseVO<Boolean> prepareOrder(@RequestBody PrepareOrderVO prepareOrderVO) {
        prepareOrderVO.setUserId(XyRequestContext.getUserId());
        return WebResponseVO.success(shopInfoService.prepareOrder(prepareOrderVO));
    }

    /**
     * 商品预热（将mysql中的库存数据加载到redis中）
     * @param anchorId
     * @return
     */
    @PostMapping("/prepareStock")
    @RequestLimit(limit = 1, second = 5, msg = "正在预热，请不要重复点击")
    public WebResponseVO<Boolean> prepareStock(Long anchorId) {
        return WebResponseVO.success(shopInfoService.prepareStock(anchorId));
    }

    /**
     * 订单支付 (这个接口主要是对同一个直播间内的所有进行结清，以直播间为维度)
     * @param prepareOrderVO
     * @return
     */
    @PostMapping("/payNow")
    public WebResponseVO<Boolean> payNow(@RequestBody PrepareOrderVO prepareOrderVO) {
        return WebResponseVO.success(shopInfoService.payNow(prepareOrderVO));
    }

    /**
     * 订单支付(用户可以选择要对具体的哪一个订单进行支付)
     * @param orderId
     * @return
     */
    @PostMapping("/payNowByOrderId")
    public WebResponseVO<Boolean> payNowByOrderId(Long orderId) {
        return WebResponseVO.success(shopInfoService.payNowByOrderId(orderId));
    }


    /**
     * 根据userId和roomId查询订单信息（可能是多条的，返回一个集合）
     * @param userId
     * @param roomId
     * @return
     */
    @PostMapping("/getOrderInfoByUserIdAndRoomId")
    public WebResponseVO<List<ShopOrderVO>> getOrderInfoByUserIdAndRoomId(Long userId, Integer roomId) {
        return WebResponseVO.success(shopInfoService.getOrderInfoByUserIdAndRoomId(userId, roomId));
    }


    /**
     * 根据userId查询订单信息（可能是多条的，返回一个集合）
     * @param userId
     * @return
     */
    @PostMapping("/getOrderInfoByUserId")
    public WebResponseVO<List<ShopOrderVO>> getOrderInfoByUserId(Long userId) {
        return WebResponseVO.success(shopInfoService.getOrderInfoByUserId(userId));
    }

    /**
     * 修改购物车中的商品的选中状态
     * @param shopCarReqVO
     * @return
     */
    @PostMapping("/updateSelectedFlag")
    public WebResponseVO<Boolean> updateSelectedFlag(@RequestBody ShopCarReqVO shopCarReqVO) {
        return WebResponseVO.success(shopInfoService.updateSelectedFlag(shopCarReqVO));
    }


    /**
     * 清空购物车(v2版本)
     * @param reqVO
     * @return
     */
    @PostMapping("/clearCar_v2")
    public WebResponseVO<Boolean> clearCar_v2(@RequestBody ShopCarReqVO reqVO) {
        return WebResponseVO.success(shopInfoService.clearCar_v2(reqVO));
    }



    /**
     * 查询购物车(V2版本)
     * @param reqVO
     * @return
     */
    @PostMapping("/getCarInfo_v2")
    public WebResponseVO<ShopCarRespVO> getCarInfo_v2(@RequestBody ShopCarReqVO reqVO) {
        return WebResponseVO.success(shopInfoService.getCarInfo_v2(reqVO));
    }


    /**
     * 从购物车中移除信息（v2，也是只操作内存版本）
     * @param reqVO
     * @return
     */
    @PostMapping("/removeFromCar_v2")
    public WebResponseVO<Boolean> removeFromCar_v2(@RequestBody ShopCarReqVO reqVO) {
        return WebResponseVO.success(shopInfoService.removeFromCar_v2(reqVO));
    }



    /**
     * 往购物车中添加商品（v2版本就是只会保存到redis）
     * @param reqVO
     * @return
     */
    @PostMapping("/addCar_v2")
    public WebResponseVO<Boolean> addCar_v2(@RequestBody ShopCarReqVO reqVO) {
        return WebResponseVO.success(shopInfoService.addCar_v2(reqVO));
    }

    /**
     * 修改购物车某一个商品数量(v2)
     * @param reqVO
     * @param changeType
     * @return
     */
    @PostMapping("/changeCarSkuNum_v2")
    public WebResponseVO<Boolean> changeCarSkuNum_v2(@RequestBody ShopCarReqVO reqVO, Integer changeType) {
        return WebResponseVO.success(shopInfoService.changeCarItemNum_v2(reqVO, changeType));
    }

}