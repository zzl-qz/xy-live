package org.xy.live.gift.constants;

public enum SkuNumChangeTypeEnum {
    ADD_NUM(1, "商品数量加1"),
    SUB_NUM(2, "商品数量减1"),
    ;

    private int code;
    private String desc;


    /**
     * 判断是否是我们定义好的数据
     * @param code
     * @return
     */
    public static boolean isContain(int code) {
        SkuNumChangeTypeEnum[] values = SkuNumChangeTypeEnum.values();
        for (SkuNumChangeTypeEnum value : values) {
            if (value.getCode() == code) {
                return true;
            }
        }
        return false;
    }



    SkuNumChangeTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
