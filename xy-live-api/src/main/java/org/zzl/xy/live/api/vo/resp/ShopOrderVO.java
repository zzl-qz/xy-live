package org.zzl.xy.live.api.vo.resp;



public class ShopOrderVO {


    private Long Id;
    private String skuIdList;
    private Long userId;
    private Integer roomId;
    private Integer status;
    private String extra;

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public String getSkuIdList() {
        return skuIdList;
    }

    public void setSkuIdList(String skuIdList) {
        this.skuIdList = skuIdList;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    @Override
    public String toString() {
        return "ShopOrderVO{" +
                "Id=" + Id +
                ", skuIdList='" + skuIdList + '\'' +
                ", userId=" + userId +
                ", roomId=" + roomId +
                ", status=" + status +
                ", extra='" + extra + '\'' +
                '}';
    }
}
