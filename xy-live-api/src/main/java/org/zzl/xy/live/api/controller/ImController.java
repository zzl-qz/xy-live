package org.zzl.xy.live.api.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xy.live.common.interfaces.vo.WebResponseVO;
import org.zzl.xy.live.api.service.ImService;
import org.zzl.xy.live.api.vo.resp.ImConfigVO;

@RestController
@RequestMapping("/im")
public class ImController {
    
    @Resource
    private ImService imService;

    /**
     * 返回IM配置信息（token、TCP和WS的配置主机）
     * @return
     */
    @PostMapping("/getImConfig")
    public WebResponseVO<ImConfigVO> getImConfig() {
        return WebResponseVO.success(imService.getImConfig());
    }
}