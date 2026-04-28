package org.xy.live.user.constants;

import java.util.concurrent.ThreadLocalRandom;

public interface RocketMQConstant {

    /**
     * 生产者线程名字
     */
    String PRODUCER_THREAD_NAME = ":rmq-producer:" + ThreadLocalRandom.current().nextInt(1000);

}


