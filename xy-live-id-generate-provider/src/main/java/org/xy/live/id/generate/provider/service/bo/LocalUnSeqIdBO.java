package org.xy.live.id.generate.provider.service.bo;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 无序
 */
public class LocalUnSeqIdBO {

    //mysql配置的id
    private Integer id;
    //对应分布式id的配置说明
    private String desc;
    //链表记录id值
    private ConcurrentLinkedQueue<Long> idQueue;
    //本地内存记录id段的开始位置
    private Long currentStart;
    //本地内存记录id段的结束位置
    private Long nextThreshold;
    //步长
    private Integer step;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public ConcurrentLinkedQueue<Long> getIdQueue() {
        return idQueue;
    }

    public void setIdQueue(ConcurrentLinkedQueue<Long> idQueue) {
        this.idQueue = idQueue;
    }

    public void setRandomIdInQueue(long begin,long end) {
        List<Long> idList = new LinkedList<>();
        for (long j = begin; j < end; j++) {
            idList.add(j);
        }
        //把队列的元素进行打乱
        Collections.shuffle(idList);
        ConcurrentLinkedQueue idQueue = new ConcurrentLinkedQueue();
        idQueue.addAll(idList);
        this.setIdQueue(idQueue);
    }

    public Long getCurrentStart() {
        return currentStart;
    }

    public void setCurrentStart(Long currentStart) {
        this.currentStart = currentStart;
    }

    public Long getNextThreshold() {
        return nextThreshold;
    }

    public void setNextThreshold(Long nextThreshold) {
        this.nextThreshold = nextThreshold;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }
}
