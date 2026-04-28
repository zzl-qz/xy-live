package org.xy.live.common.interfaces.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 集合工具类
 */
public class ListUtils {


    /**
     * 将一个 大list 拆成每组最多只有 splitNum个元素 的小集合
     * @param list
     * @param splitNum
     * @return
     * @param <T>
     */
    public static <T> List<List<T>> splitList(List<T> list, int splitNum) {
        List<List<T>> result = new ArrayList<>();
        int size = list.size();
        int groupNum = (size / splitNum); // 前面整除部分要拆分多少组
        int preCursor = 0; // 每一次遍历的起始元素
        int lastCursor = preCursor + splitNum;  // 每一次遍历的结束元素
        // 前面的 splitNum 组就按照每组 groupNum 个来就行
        // (主要是考虑到用户给的这个集合可能是150个元素，但是如果说每组是10个，
        // 那么最后50个最好还是再开一个集合去存储比较好，否则极端情况下会出现存储了接近两倍的splitNum【2 * splitNum - 1】)
        List<T> tmp;
        for (int i = 0; i < groupNum; i++) {
            tmp = list.subList(preCursor, lastCursor);
            result.add(tmp);
            preCursor = lastCursor;
            lastCursor = preCursor + splitNum;
        }
        // 最后剩余多少都直接塞到集合中
        if (preCursor != size) {
            result.add(list.subList(preCursor, size));
        }
        return result;
    }


}
