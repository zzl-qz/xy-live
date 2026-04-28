package org.zzl.xy.live.api.vo.resp;

import java.util.Date;

public class DanmakuVO {

    private Long id;

    /**
     * 直播间ID（关联直播间表）
     */
    private Integer roomId;

    /**
     * 发送用户ID（关联用户表）
     */
    private Long userId;

    /**
     * 弹幕内容
     */
    private String content;

    /**
     * 发送时间（精确到毫秒）
     */
    private Date sendTime;

    /**
     * 弹幕唯一id
     */
    private String mid;

    /**
     * 弹幕类型：0-普通文本，1-礼物弹幕，2-系统通知
     */
    private Integer type;

    /**
     * 弹幕颜色（如#FFFFFF）
     */
    private String color;

    /**
     * 字体大小（默认16px）
     */
    private Integer fontSize;

    /**
     * 显示位置：0-滚动，1-顶部，2-底部
     */
    private Integer position;

    /**
     * 是否敏感内容：0-正常，1-敏感（需过滤）
     */
    private Integer isSensitive;

    /**
     * 敏感原因（如包含违规词）
     */
    private String sensitiveReason;

    /**
     * 状态：0-删除，1-正常
     */
    private Integer status;

}
