package org.xy.live.gitf.provider.bo;

/**
 * @Author: Daylight
 * @Date: 2026-01-28 16:54
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送同步红包雨活动数据BO对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyncRedPackDataBO {

    private String code;
    private int cnt;

}
