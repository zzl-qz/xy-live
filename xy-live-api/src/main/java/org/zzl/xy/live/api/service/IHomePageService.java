package org.zzl.xy.live.api.service;


import org.zzl.xy.live.api.vo.HomePageVO;

public interface IHomePageService {

    /**
     * 获取初始化用户页面信息
     *
     * @param userId
     * @return
     */
    HomePageVO initPage(Long userId);
}