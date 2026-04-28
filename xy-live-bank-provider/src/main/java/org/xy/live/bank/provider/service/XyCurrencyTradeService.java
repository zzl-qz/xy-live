package org.xy.live.bank.provider.service;


import org.xy.live.bank.interfaces.dto.XyCurrencyTradeDTO;

/**
 * 流水记录服务层
 */
public interface XyCurrencyTradeService {

    /**
     * 插入一条流水记录
     *
     * @param userId
     * @param num
     * @param type
     * @param extraCol
     * @return
     */
    boolean insertOne(long userId,int num,int type, String extraCol);


    /**
     * 根据扩展字段进行查询
     * @param code
     * @param extraCol
     * @return
     */
    XyCurrencyTradeDTO getByExtraCol(int code, String extraCol);
}
