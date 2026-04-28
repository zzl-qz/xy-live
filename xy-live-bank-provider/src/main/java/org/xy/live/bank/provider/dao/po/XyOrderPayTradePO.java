package org.xy.live.bank.provider.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_xy_order_pay_trade")
public class XyOrderPayTradePO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId; // 订单Id（业务id）
    private Long userId;
    private Integer num;
    private Integer status;
    private String remark; // 拓展字段
    private Date createTime;
    private Date updateTime;
}
