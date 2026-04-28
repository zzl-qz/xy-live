package org.xy.live.bank.provider.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_xy_currency_trade")
public class XyCurrencyTradePO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer type;
    private Integer status;
    private Integer num;
    private String extraCol;
    private Date createTime;
    private Date updateTime;
}
