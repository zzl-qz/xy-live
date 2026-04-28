package org.zzl.xy.live.api.vo;

import lombok.Data;
import org.zzl.xy.live.api.annotations.DecryptId;
import org.zzl.xy.live.api.annotations.EncryptId;

@Data
public class LivingRoomInitVO {


    private Long anchorId;

    private Long userId;
    private String anchorImg;
    private String roomName;
    private boolean isAnchor;
    private String redPacketConfigCode; // 红包雨唯一标识（如果有的话）
    private String avatar;

    private Integer roomId;
    private String watcherNickName;
    private String anchorNickName;
    //观众头像
    private String watcherAvatar;
    //默认背景图
    private String defaultBgImg;

    private Long pkObjId;
    private String nickName;
}