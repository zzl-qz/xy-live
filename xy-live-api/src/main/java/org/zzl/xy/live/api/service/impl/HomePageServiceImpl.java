package org.zzl.xy.live.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.xy.live.user.dto.UserDTO;
import org.xy.live.user.interfaces.rpc.IUserRpc;
import org.xy.live.user.interfaces.rpc.IUserTagRpc;
import org.zzl.xy.live.api.service.IHomePageService;
import org.zzl.xy.live.api.vo.HomePageVO;

import static org.xy.live.user.constants.UserTagsEnum.IS_VIP;

@Service
@Slf4j
public class HomePageServiceImpl implements IHomePageService {

    @DubboReference(timeout = 15000)
    private IUserRpc userRpc;

    @DubboReference(timeout = 15000)
    private IUserTagRpc userTagRpc;

    @Override
    public HomePageVO initPage(Long userId) {
        // 远程调用获取用户信息DTO
        UserDTO userDTO = userRpc.getByUserId(userId);
        // 构建HomePageVO对象
        HomePageVO homePageVO = new HomePageVO();
        homePageVO.setUserId(userId);
        homePageVO.setNickName(userDTO.getNickName());
        homePageVO.setAvatar(userDTO.getAvatar());
        // 这里关于是否是主播，我们是通过是否有VIP标签来确定的（主播的话前端会展示出一个开播的按钮）
        homePageVO.setShowStartLivingBtn(userTagRpc.containTag(userId, IS_VIP));
        return homePageVO;
    }
}
