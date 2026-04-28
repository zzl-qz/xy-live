package org.xy.live.im.router.constants;



/**
 * @see org.xy.live.im.constants.ImMsgCodeEnum
 * 虽然我们前面已经定义了一个枚举ImMsgCodeEnum，但是这个只是用来对外层的进行区分是什么类型的消息，其中有一个是业务消息
 * 但是业务消息也是分为多种的，比如是聊天类型还是送礼类型，所以这个类定义了各个业务消息内部的bizCode进行区分
 */

public enum ImMsgBizCodeEnum {
    
    LIVING_ROOM_IM_CHAT_MSG_BIZ(5555, "直播间im聊天消息"),
    LIVING_ROOM_SEND_GIFT_SUCCESS(5556, "送礼成功"),
    LIVING_ROOM_SEND_GIFT_FAIL(5557, "送礼失败"),
    LIVING_ROOM_PK_SEND_GIFT_SUCCESS(5558,"pk送礼成功"),
    LIVING_ROOM_PK_ONLINE(5559,"pk连线"),
    START_RED_PACKET(5560,"红包雨活动开始了"),
    ;

    int code;
    String desc;

    ImMsgBizCodeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}