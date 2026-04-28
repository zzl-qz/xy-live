package org.zzl.xy.live.api.controller;


import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xy.live.common.interfaces.vo.WebResponseVO;
import org.xy.live.web.starter.context.XyRequestContext;
import org.zzl.xy.live.api.service.IHomePageService;
import org.zzl.xy.live.api.vo.HomePageVO;

/**
 * 这个控制器是用来告诉前端是否已经登录成功并携带token
 */
@RestController
@RequestMapping("/home")
public class HomePageController {

    @Resource
    private IHomePageService homePageService;

    /**
     * 初始化用户页面信息
     * @return
     */
    @PostMapping("/initPage")
    public WebResponseVO<HomePageVO> initPage() {
        Long userId = XyRequestContext.getUserId();
        HomePageVO homePageVO = new HomePageVO();
        homePageVO.setLoginStatus(false);
        if (userId != null) {
            homePageVO = homePageService.initPage(userId);
            homePageVO.setLoginStatus(true);
        }
        //前端调用initPage --> success状态则代表已经登录过，token有效，前端可隐藏登录按钮
        return WebResponseVO.success(homePageVO);
    }
}