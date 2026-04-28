package org.zzl.xy.live.api.service.impl;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.gift.dto.*;
import org.xy.live.gift.rpc.IShopCarRpc;
import org.xy.live.gift.rpc.ISkuInfoRpc;
import org.xy.live.gift.rpc.ISkuOrderInfoRpc;
import org.xy.live.gift.rpc.ISkuStockInfoRpc;
import org.xy.live.web.starter.context.XyRequestContext;
import org.zzl.xy.live.api.service.IShopInfoService;
import org.zzl.xy.live.api.vo.req.ShopCarReqVO;
import org.zzl.xy.live.api.vo.req.SkuInfoReqVO;
import org.zzl.xy.live.api.vo.resp.*;

import java.util.List;

@Service
public class ShopInfoServiceImpl implements IShopInfoService {

    @DubboReference(timeout = 15000)
    private ISkuInfoRpc skuInfoRpc;
    
    @DubboReference(timeout = 15000)
    private IShopCarRpc shopCarRpc;

    @DubboReference(timeout = 15000)
    private ISkuOrderInfoRpc skuOrderInfoRpc;

    @DubboReference(timeout = 15000)
    private ISkuStockInfoRpc skuStockInfoRpc;

    @Override
    public List<SkuInfoVO> queryByAnchorId(Long anchorId) {
        List<SkuInfoDTO> skuInfoDTOS = skuInfoRpc.queryByAnchorId(anchorId);
        return ConvertBeanUtils.convertList(skuInfoDTOS, SkuInfoVO.class);
    }

    @Override
    public SkuDetailInfoVO detail(SkuInfoReqVO skuInfoReqVO) {
        return ConvertBeanUtils.convert(skuInfoRpc.queryBySkuId(skuInfoReqVO.getSkuId(), skuInfoReqVO.getAnchorId()), SkuDetailInfoVO.class);
    }

    @Override
    public Boolean addCar(ShopCarReqVO reqVO) {
        return shopCarRpc.addCar(new ShopCarReqDTO(XyRequestContext.getUserId(), reqVO.getSkuId(), reqVO.getRoomId()));
    }

    @Override
    public Boolean removeFromCar(ShopCarReqVO reqVO) {
        return shopCarRpc.removeFromCar(new ShopCarReqDTO(XyRequestContext.getUserId(), reqVO.getSkuId(), reqVO.getRoomId()));
    }

    @Override
    public Boolean clearShopCar(ShopCarReqVO reqVO) {
        return shopCarRpc.clearShopCar(new ShopCarReqDTO(XyRequestContext.getUserId(), reqVO.getSkuId(), reqVO.getRoomId()));
    }

    @Override
    public Boolean changeCarItemNum(ShopCarReqVO reqVO, Integer changeType) {
        return shopCarRpc.changeCarItemNum(new ShopCarReqDTO(XyRequestContext.getUserId(), reqVO.getSkuId(), reqVO.getRoomId()), changeType);
    }

    @Override
    public ShopCarRespVO getCarInfo(ShopCarReqVO reqVO) {
        ShopCarRespDTO carInfo = shopCarRpc.getCarInfo(new ShopCarReqDTO(XyRequestContext.getUserId(),
                reqVO.getSkuId(), reqVO.getRoomId()));
        ShopCarRespVO shopCarRespVO = ConvertBeanUtils.convert(carInfo, ShopCarRespVO.class);
        shopCarRespVO.setShopCarItemRespDTOS(carInfo.getSkuCarItemRespDTOS());
        return shopCarRespVO;
    }

    @Override
    public boolean prepareOrder(PrepareOrderVO prepareOrderVO) {
        PrepareOrderReqDTO reqDTO = new PrepareOrderReqDTO();
        reqDTO.setRoomId(prepareOrderVO.getRoomId());
        reqDTO.setUserId(prepareOrderVO.getUserId());
        skuOrderInfoRpc.prepareOrder(reqDTO);
        return true;
    }

    @Override
    public boolean prepareStock(Long anchorId) {
        return skuStockInfoRpc.prepareStockInfo(anchorId);
    }

    @Override
    public boolean payNow(PrepareOrderVO prepareOrderVO) {
        return skuOrderInfoRpc.payNow(XyRequestContext.getUserId(), prepareOrderVO.getRoomId());
    }

    @Override
    public boolean payNowByOrderId(Long orderId) {
        return skuOrderInfoRpc.payNowByOrderId(orderId);
    }

    @Override
    public List<ShopOrderVO> getOrderInfoByUserIdAndRoomId(Long userId, Integer roomId) {
        return ConvertBeanUtils.convertList(skuOrderInfoRpc.queryByUserIdAndRoomId(userId, roomId), ShopOrderVO.class);
    }

    @Override
    public List<ShopOrderVO> getOrderInfoByUserId(Long userId) {
        return ConvertBeanUtils.convertList(skuOrderInfoRpc.queryByUserId(userId), ShopOrderVO.class);
    }

    @Override
    public Boolean updateSelectedFlag(ShopCarReqVO shopCarReqVO) {
        return shopCarRpc.updateSelectedFlag(new ShopCarReqDTO(XyRequestContext.getUserId(), shopCarReqVO.getSkuId(), shopCarReqVO.getRoomId()));
    }

    @Override
    public Boolean clearCar_v2(ShopCarReqVO reqVO) {
        return shopCarRpc.clearShopCar_v2(new ShopCarReqDTO(XyRequestContext.getUserId(), reqVO.getSkuId(), reqVO.getRoomId()));
    }

    @Override
    public ShopCarRespVO getCarInfo_v2(ShopCarReqVO reqVO) {
        ShopCarRespDTO carInfo = shopCarRpc.getCarInfo_v2(new ShopCarReqDTO(XyRequestContext.getUserId(),
                reqVO.getSkuId(), reqVO.getRoomId()));
        ShopCarRespVO shopCarRespVO = ConvertBeanUtils.convert(carInfo, ShopCarRespVO.class);
        shopCarRespVO.setShopCarItemRespDTOS(carInfo.getSkuCarItemRespDTOS());
        return shopCarRespVO;
    }

    @Override
    public Boolean removeFromCar_v2(ShopCarReqVO reqVO) {
        return shopCarRpc.removeFromCar_v2(new ShopCarReqDTO(XyRequestContext.getUserId(),
                reqVO.getSkuId(), reqVO.getRoomId()));
    }

    @Override
    public Boolean addCar_v2(ShopCarReqVO reqVO) {
        return shopCarRpc.addCar_v2(new ShopCarReqDTO(XyRequestContext.getUserId(),
                reqVO.getSkuId(), reqVO.getRoomId()));
    }

    @Override
    public Boolean changeCarItemNum_v2(ShopCarReqVO reqVO, Integer changeType) {
        return shopCarRpc.changeCarItemNum_v2(new ShopCarReqDTO(XyRequestContext.getUserId(),
                reqVO.getSkuId(), reqVO.getRoomId()), changeType);
    }
}