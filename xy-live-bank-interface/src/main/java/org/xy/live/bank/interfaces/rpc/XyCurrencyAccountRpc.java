package org.xy.live.bank.interfaces.rpc;


import org.xy.live.bank.interfaces.dto.AccountTradeReqDTO;
import org.xy.live.bank.interfaces.dto.AccountTradeRespDTO;
import org.xy.live.bank.interfaces.dto.XyCurrencyAccountDTO;

public interface XyCurrencyAccountRpc {

    /**
     * 新增账户
     */
    boolean insertOne(Long userId);

    /**
     * 增加虚拟货币
     */
    void incr(Long userId, int num, int code, String extraCol);

    /**
     * 扣减虚拟币
     */
    boolean decr(Long userId, int num, int code, String extraCol);

    /**
     * 查询账户
     */
    XyCurrencyAccountDTO getByUserId(Long userId);

    /**
     * 查询账户余额
     */
    Integer getBalance(Long userId);

    /**
     * 专门给送礼用的扣减库存逻辑，进行了高并发优化
     */
    AccountTradeRespDTO consumeForSendGift(AccountTradeReqDTO accountTradeReqDTO);
}