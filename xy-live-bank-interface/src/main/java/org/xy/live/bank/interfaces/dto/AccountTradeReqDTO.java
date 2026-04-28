package org.xy.live.bank.interfaces.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AccountTradeReqDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7722121828825334678L;
    private long userId;
    private int num;

}
