package org.xy.live.id.generate.provider.utils;

public final class BitPermutationUtil {

    // 固定 32→32 的查表置换（可自行更换，但上线后不要变）
    private static final int[] P = {
        7,22,13,31,5,17,9,25,
        3,28,19,11,29,2,23,14,
        30,6,16,4,27,12,24,1,
        18,10,26,0,15,8,20,21
    };

    // 正向混淆
    public static long mask(long seq) {
        long low = seq & 0xFFFFFFFFL; // 取出低32位
        long out = 0;
        for (int i = 0; i < 32; i++) {
            out |= ((low >>> P[i]) & 1) << i;
        }
        // 高 32 位原样保留，低 32 位被置换
        return (seq & 0xFFFFFFFF00000000L) | out;
    }

    // 反解（可选，如果业务需要）
    public static long unmask(long masked) {
        long low = masked & 0xFFFFFFFFL;
        long origin = 0;
        for (int i = 0; i < 32; i++) {
            origin |= ((low >>> i) & 1) << P[i];
        }
        return (masked & 0xFFFFFFFF00000000L) | origin;
    }

    private BitPermutationUtil() {}
}