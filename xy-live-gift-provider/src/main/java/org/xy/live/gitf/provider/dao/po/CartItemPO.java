package org.xy.live.gitf.provider.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_cart_item")
public class CartItemPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long skuId;
    private Integer qty;
    private Integer selected;
    private Long roomId;
    private Integer status;
    private Date createTime;
    private Date updateTime;
    private Date lastViewTime; // 最后浏览时间
}