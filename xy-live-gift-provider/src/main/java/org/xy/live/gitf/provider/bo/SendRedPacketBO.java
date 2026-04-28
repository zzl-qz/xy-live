package org.xy.live.gitf.provider.bo;

import lombok.Data;
import org.xy.live.gift.dto.RedPacketConfigReqDTO;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SendRedPacketBO implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1829802295999336708L;
    
    private Integer price;
    private RedPacketConfigReqDTO reqDTO;
}