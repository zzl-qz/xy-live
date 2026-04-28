package org.xy.live.gitf.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.xy.live.common.interfaces.enums.CommonStatusEnum;
import org.xy.live.gitf.provider.dao.mapper.IAnchorShopInfoMapper;
import org.xy.live.gitf.provider.dao.po.AnchorShopInfoPO;
import org.xy.live.gitf.provider.service.IAnchorShopInfoService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnchorShopInfoServiceImpl implements IAnchorShopInfoService {


    @Resource
    private IAnchorShopInfoMapper anchorShopInfoMapper;

    @Override
    public List<Long> querySkuIdsByAnchorId(Long anchorId) {
        LambdaQueryWrapper<AnchorShopInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AnchorShopInfoPO::getAnchorId, anchorId);
        queryWrapper.eq(AnchorShopInfoPO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        List<AnchorShopInfoPO> anchorShopInfoPOS = anchorShopInfoMapper.selectList(queryWrapper);
        return anchorShopInfoPOS.stream().map(AnchorShopInfoPO::getSkuId).collect(Collectors.toList());
    }

    @Override
    public List<Long> queryAllValidAnchorId() {
        LambdaQueryWrapper<AnchorShopInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AnchorShopInfoPO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        List<AnchorShopInfoPO> anchorShopInfoPOS = anchorShopInfoMapper.selectList(queryWrapper);
        return anchorShopInfoPOS.stream().map(AnchorShopInfoPO::getAnchorId).distinct().collect(Collectors.toList());
    }
}
