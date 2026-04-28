package org.xy.live.im.core.server.common;


import lombok.Data;
import org.xy.live.im.constants.ImConstants;

import java.io.Serial;
import java.io.Serializable;

/**
 * Netty消息体
 */
@Data
public class ImMsg implements Serializable {
    @Serial
    private static final long serialVersionUID = -7007538930769644633L;

    //魔数：用于做基本校验
    private short magic;
    
    //用于记录body的长度(主要是在编解码器中会用到)
    private int len;
    
    //用于标识当前消息的作用（类型），后序交给不同的handler去处理
    private int code;
    
    //存储消息体的内容，一般会按照字节数组的方式去存放
    private byte[] body;

    // private String mid; // 消息体id，幂等操作

    public static ImMsg build(int code, String data) {
        ImMsg imMsg = new ImMsg();
        imMsg.setMagic(ImConstants.DEFAULT_MAGIC);
        imMsg.setCode(code);
        imMsg.setBody(data.getBytes());
        imMsg.setLen(imMsg.getBody().length);
        return imMsg;
    }
}