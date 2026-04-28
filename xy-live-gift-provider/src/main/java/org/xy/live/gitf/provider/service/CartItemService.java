package org.xy.live.gitf.provider.service;

import org.xy.live.gift.dto.CartItemRespDTO;
import org.xy.live.gitf.provider.bo.CartChangeEventBO;

import java.util.List;

public interface CartItemService {

    /**
     * 将redis的购物车数据同步到mysql中
     * @param cartChangeEventBO
     * @return
     */
    boolean syncRedisToMysqlHandle(CartChangeEventBO cartChangeEventBO);

    /**
     * 清空购物车
     * @param cartChangeEventBO
     * @return
     */
    boolean clearCarHandle(CartChangeEventBO cartChangeEventBO);

    /**
     * 修改最后访问时间
     * @param userId
     */
    void updateLastLiveAt(Long userId);


    /**
     * 根据用户id查询信息
     * @param userId
     * @return
     */
    List<CartItemRespDTO> selectByUserIdByDB(Long userId);

    /**
     * 查询冷用户Id
     * @return
     */
    List<Long> selectColdUserId();
}
