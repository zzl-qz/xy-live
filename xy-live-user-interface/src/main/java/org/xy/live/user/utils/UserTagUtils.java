package org.xy.live.user.utils;

public class UserTagUtils {

    /**
     * 是否包含某个标签（与操作）
     * @param userTag 用户当前的标签
     * @param expectTag 希望包含的标签
     * @return
     */
    public static boolean isContain(Long userTag, Long expectTag) {
        return userTag != null && expectTag != null && expectTag > 0 && (userTag & expectTag) == expectTag;
    }

}
