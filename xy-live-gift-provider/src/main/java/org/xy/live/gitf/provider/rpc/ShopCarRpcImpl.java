package org.xy.live.gitf.provider.rpc;

import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.gift.dto.ShopCarReqDTO;
import org.xy.live.gift.dto.ShopCarRespDTO;
import org.xy.live.gift.rpc.IShopCarRpc;
import org.xy.live.gitf.provider.service.IShopCarService;

@DubboService
public class ShopCarRpcImpl implements IShopCarRpc {

    @Resource
    private IShopCarService shopCarService;

    @Override
    public Boolean addCar(ShopCarReqDTO shopCarReqDTO) {
        return shopCarService.addCar(shopCarReqDTO);
    }

    @Override
    public Boolean removeFromCar(ShopCarReqDTO shopCarReqDTO) {
        return shopCarService.removeFromCar(shopCarReqDTO);
    }

    @Override
    public Boolean clearShopCar(ShopCarReqDTO shopCarReqDTO) {
        return shopCarService.clearShopCar(shopCarReqDTO);
    }

    @Override
    public Boolean changeCarItemNum(ShopCarReqDTO shopCarReqDTO, Integer changeType) {
        return shopCarService.changeCarItemNum(shopCarReqDTO, changeType);
    }
    @Override
    public ShopCarRespDTO getCarInfo(ShopCarReqDTO shopCarReqDTO) {
        return shopCarService.getCarInfo(shopCarReqDTO);
    }

    @Override
    public Boolean updateSelectedFlag(ShopCarReqDTO shopCarReqDTO) {
        return shopCarService.updateSelectedFlag(shopCarReqDTO);
    }

    @Override
    public Boolean clearShopCar_v2(ShopCarReqDTO reqVO) {
        return shopCarService.clearShopCar_v2(reqVO);
    }

    @Override
    public ShopCarRespDTO getCarInfo_v2(ShopCarReqDTO reqVO) {
        return shopCarService.getCarInfo_v2(reqVO);
    }

    @Override
    public Boolean removeFromCar_v2(ShopCarReqDTO reqVO) {
        return shopCarService.removeFromCar_v2(reqVO);
    }

    @Override
    public Boolean addCar_v2(ShopCarReqDTO reqVO) {
        return shopCarService.addCar_v2(reqVO);
    }

    @Override
    public Boolean changeCarItemNum_v2(ShopCarReqDTO shopCarReqDTO, Integer changeType) {
        return shopCarService.changeCarItemNum_v2(shopCarReqDTO, changeType);
    }
}