package org.zzl.xy.live.api.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xy.live.common.interfaces.vo.WebResponseVO;
import org.zzl.xy.live.api.service.IGiftService;
import org.zzl.xy.live.api.vo.req.GiftReqVO;
import org.zzl.xy.live.api.vo.resp.GiftConfigVO;

import java.util.List;

@RestController
@RequestMapping("/gift")
public class GiftController {
    
    @Resource
    private IGiftService giftService;
    
    @PostMapping("/listGift")
    public WebResponseVO<List<GiftConfigVO>> listGift() {
        List<GiftConfigVO> giftConfigVOS = giftService.listGift();
        return WebResponseVO.success(giftConfigVOS);
    }
    
    @PostMapping("/send")
    public WebResponseVO<Boolean> send(@RequestBody GiftReqVO giftReqVO) {
        return WebResponseVO.success(giftService.send(giftReqVO));
    }


}