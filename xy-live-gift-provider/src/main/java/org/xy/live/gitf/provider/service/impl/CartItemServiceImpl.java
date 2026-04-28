package org.xy.live.gitf.provider.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.xy.live.common.interfaces.enums.CommonStatusEnum;
import org.xy.live.common.interfaces.utils.ConvertBeanUtils;
import org.xy.live.framework.redis.starter.key.GiftProviderCacheKeyBuilder;
import org.xy.live.gift.dto.CartItemRespDTO;
import org.xy.live.gitf.provider.bo.CartChangeEventBO;
import org.xy.live.gitf.provider.dao.mapper.CartItemMapper;
import org.xy.live.gitf.provider.dao.po.CartItemPO;
import org.xy.live.gitf.provider.service.CartItemService;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartItemServiceImpl implements CartItemService {

    @Resource
    private CartItemMapper cartItemMapper;

    @Override
    public boolean syncRedisToMysqlHandle(CartChangeEventBO e) {
        if (e.getQty() == null) {           // 删除
            this.delete(e.getUserId(), e.getSkuId());
        } else {                            // 新增/更新
            this.upsert(e.getUserId(), e.getSkuId(), e.getSelected(), e.getQty(), e.getRoomId());
        }
        return true;
    }


    @Override
    public boolean clearCarHandle(CartChangeEventBO e) {
        cartItemMapper.clear(e.getUserId());
        return true;
    }

    /**
     * 修改或是增加
     * @param userId
     * @param skuId
     * @param selected
     * @param qtk
     * @return
     */
    private void upsert(Long userId, Long skuId, Boolean selected, Integer qtk, Integer roomId) {
        cartItemMapper.upsert(userId, skuId, qtk, selected, roomId);
    }

    /**
     * 删除
     * @param userId
     * @param skuId
     * @return
     */
    private void delete(Long userId, Long skuId) {
        cartItemMapper.delete(userId, skuId);
    }


    /**
     * 修改最后访问时间
     * @param userId
     */
    public void updateLastLiveAt(Long userId) {
        cartItemMapper.touchLastView(userId);
    }

    /**
     * 根据用户id查询信息
     * @param userId
     * @return
     */
    public List<CartItemRespDTO> selectByUserIdByDB(Long userId) {
        LambdaQueryWrapper<CartItemPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItemPO::getUserId, userId);
        queryWrapper.eq(CartItemPO::getStatus, CommonStatusEnum.VALID_STATUS.getCode());
        return ConvertBeanUtils.convertList(cartItemMapper.selectList(queryWrapper), CartItemRespDTO.class);
    }


    /**
     * 查询冷用户Id
     * @return
     */
    public List<Long> selectColdUserId() {
        return cartItemMapper.selectColdUserId();
    }


}
