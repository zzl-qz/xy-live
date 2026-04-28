package org.xy.live.bank.provider.service;

import org.xy.live.bank.provider.dao.po.PayTopicPO;

public interface IPayTopicService {

    /**
     * 根据业务码获取PayTopicPO
     * @param code
     * @return
     */
    PayTopicPO getByCode(Integer code);
}