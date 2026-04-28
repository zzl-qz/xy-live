package org.xy.live.bank.provider.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_xy_currency_account")
public class XyCurrencyAccountPO {
    
    @TableId(type = IdType.INPUT)
    private Long userId;
    private int currentBalance;
    private int totalCharged;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}