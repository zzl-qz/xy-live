package org.zzl.xy.live.api.service;

import org.zzl.xy.live.api.vo.req.GiftReqVO;
import org.zzl.xy.live.api.vo.resp.GiftConfigVO;

import java.util.List;

public interface IGiftService {

    List<GiftConfigVO> listGift();

    boolean send(GiftReqVO giftReqVO);
}