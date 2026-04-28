package org.xy.live.id.generate.provider.service.impl;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.xy.live.id.generate.provider.service.IdGenerateService;
import org.xy.live.id.generate.provider.service.bo.LocalSeqIdBO;
import org.xy.live.id.generate.provider.service.bo.LocalUnSeqIdBO;
import org.xy.live.id.generate.provider.service.dao.mapper.IdBuilderMapper;
import org.xy.live.id.generate.provider.service.dao.po.IdBuilderPO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.xy.live.id.generate.provider.contanst.ThreadContanst.ID_GENERATE_THREAD;

@Service
public class IdGenerateServiceImpl implements IdGenerateService, InitializingBean {

    @Resource
    private IdBuilderMapper idBuilderMapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(IdGenerateServiceImpl.class);

    // 触发刷新因子
    private static final float SEQ_REFRESH_FACTOR = 0.75f;

    private static final float UNSEQ_REFRESH_FACTOR = 0.25f;

    // 自定义线程池
    private static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            8, 16, 3, TimeUnit.MINUTES, new ArrayBlockingQueue<>(1000), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(ID_GENERATE_THREAD);
            return thread;
        }
    });

    /**
     * 存储每一个localSeqIdBOMap对象（每个对象能拿到的id数和设置的步长有关）
     */
    private Map<Integer, LocalSeqIdBO> localSeqIdBOMap = new ConcurrentHashMap<>();

    /**
     * 存储每一个LocalUnSeqIdBO对象
     */
    private Map<Integer, LocalUnSeqIdBO> localUnSeqIdBOMap = new ConcurrentHashMap<>();

    /**
     * 实现线程限流（控制进入执行的线程数量）
     */
    private Map<Integer, Semaphore> semaphoreMap = new ConcurrentHashMap<>();

    // 有序
    private static final int IS_SEQ = 1;


    /**
     * 获取有序Id
     * @param code
     * @return
     */
    @Override
    public Long getSeqId(Integer code) {
        if (code == null) {
            LOGGER.error("code不能为空");
            return null;
        }
        LocalSeqIdBO localSeqIdBO = localSeqIdBOMap.get(code);
        if (localSeqIdBO == null) {
            LOGGER.error("localSeqIdBO对象不能为空");
            return null;
        }
        // 判断是否需要刷新
        this.refreshLocalSeqId(localSeqIdBO);
        // 判断避免超了
        if (localSeqIdBO.getCurrentValue().get() > localSeqIdBO.getNextThreshold()) {
            LOGGER.error("当前ID段已经用完了，等待刷新");
            return null;
        }
        long resId = localSeqIdBO.getCurrentValue().incrementAndGet();
        return resId;
    }


    /**
     * 获取无序Id
     * @param code
     * @return
     */
    @Override
    public Long getUnSeqId(Integer code) {
        if (code == null) {
            LOGGER.error("code不能为空");
            return null;
        }
        LocalUnSeqIdBO localUnSeqIdBO = localUnSeqIdBOMap.get(code);
        if (localUnSeqIdBO == null) {
            LOGGER.error("localUnSeqIdBO对象不能为空");
            return null;
        }
        Long resId = localUnSeqIdBO.getIdQueue().poll();
        if (resId == null) {
            LOGGER.error("resId为空");
            return null;
        }
        // 判断是否需要刷新
        this.refreshLocalUnSeqId(localUnSeqIdBO);
        return resId;
    }

    /**
     * 刷新本地有序字段ID
     * @param localSeqIdBO
     */
    private void refreshLocalSeqId(LocalSeqIdBO localSeqIdBO) {
        long step = localSeqIdBO.getNextThreshold() - localSeqIdBO.getCurrentStart();
        // 当前使用的id需要已经超过此次id的75%
        if (localSeqIdBO.getCurrentValue().get() - localSeqIdBO.getCurrentStart() > step * SEQ_REFRESH_FACTOR) {
            // 从semaphoreMap取出数据，判断是否有线程正在进行初始化
            Semaphore semaphore = semaphoreMap.get(localSeqIdBO.getId());
            if (semaphore == null) { // 说明没有初始化载这种Id策略
                LOGGER.error("semaphore 为 空，没有加载这种ID生成策略");
                return;
            }
            // 尝试进入线程池 tryAcquire() 不会造成阻塞
            boolean tryAcquire = semaphoreMap.get(localSeqIdBO.getId()).tryAcquire();
            if (tryAcquire) { // 能进入说明没有别的线程在进行刷新
                LOGGER.info("开始进行刷新");

                // 利用线程池异步刷新
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            IdBuilderPO builderPO = idBuilderMapper.selectById(localSeqIdBO.getId()); // 查询出最新的数据
                            tryUpdateMysqlRecord(builderPO); // 触发刷新
                        } catch (Exception e) {
                            LOGGER.error("[refreshLocalSeqId] err, errMsg is {}", e.getMessage());
                        } finally {
                            // 将semaphore释放掉，别的线程才能进入
                            semaphoreMap.get(localSeqIdBO.getId()).release();
                        }
                    }
                });
            }
        }
    }

    /**
     * 刷新本地无序Id
     * @param localUnSeqIdBO
     */
    private void refreshLocalUnSeqId(LocalUnSeqIdBO localUnSeqIdBO) {
        // 先判断使用的范围
        Long start = localUnSeqIdBO.getCurrentStart();
        Long end = localUnSeqIdBO.getNextThreshold();
        int size = localUnSeqIdBO.getIdQueue().size();
        // 不足25%进行刷新
        if (size < (end - start) * UNSEQ_REFRESH_FACTOR) {
            Semaphore semaphore = semaphoreMap.get(localUnSeqIdBO.getId());
            if (semaphore == null) { // 说明没有加初始化载这种Id策略
                LOGGER.error("semaphore 为 空，没有加载这种ID生成策略");
                return;
            }
            // 尝试进入线程池 tryAcquire() 不会造成阻塞
            boolean tryAcquire = semaphoreMap.get(localUnSeqIdBO.getId()).tryAcquire();
            if (tryAcquire) { // 能进入说明没有别的线程在进行刷新
                LOGGER.info("开始进行刷新");

                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            IdBuilderPO builderPO = idBuilderMapper.selectById(localUnSeqIdBO.getId()); // 查询出最新的数据
                            tryUpdateMysqlRecord(builderPO); // 触发刷新
                            LOGGER.info("触发刷新");
                        } catch (Exception e) {
                            LOGGER.error("[refreshLocalUnSeqId] err, errMsg is {}", e.getMessage());
                        } finally {
                            semaphoreMap.get(localUnSeqIdBO.getId()).release();
                        }
                    }
                });

            }


        }
    }

    /**
     * 容器初始化的时候就会回调到这里
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info("开始进行初始化");
        // 去遍历数据库
        List<IdBuilderPO> idBuilderPOS = idBuilderMapper.selectAll();
        for (IdBuilderPO idBuilderPO : idBuilderPOS) {
            tryUpdateMysqlRecord(idBuilderPO);
            // 限制每次只能有一个线程来执行初始化的操作，初始化的时候把每一个类型的id生成策略都放进来
            semaphoreMap.put(idBuilderPO.getId(), new Semaphore(1));
        }
    }


    /**
     * 初始化ID数据段操作步骤
     * @param idBuilderPO
     */
    private void tryUpdateMysqlRecord(IdBuilderPO idBuilderPO) {
        // 告诉数据库这个已经被占用了
        Integer updatedRes = idBuilderMapper.updateNewVersion(idBuilderPO.getId(), idBuilderPO.getVersion());
        if (updatedRes > 0) {
            // 成功
            buildIdBO(idBuilderPO);
        }
        // 如果失败的话就需要进行重试
        for (int i = 0; i < 3; i++) { // 重试3次
            // 获取该策略最新的数据（因为可能已经被修改过了）
            idBuilderPO = idBuilderMapper.selectById(idBuilderPO.getId());
            // 重新尝试获取新的Id段
            updatedRes = idBuilderMapper.updateNewVersion(idBuilderPO.getId(), idBuilderPO.getVersion());
            if (updatedRes > 0) { // 数据获取成功
                buildIdBO(idBuilderPO);
                return;
            }
        }
        // 三次都没成功
        throw new RuntimeException("当前比较繁忙，ID占领失败, ID:" + idBuilderPO.getId());
    }

    /**
     * 处理构建IdBO
     * @param idBuilderPO
     */
    private void buildIdBO(IdBuilderPO idBuilderPO) {
        long currentStart = idBuilderPO.getCurrentStart();
        long nextThreshold = idBuilderPO.getNextThreshold();
        long currentNum = currentStart;
        if (idBuilderPO.getIsSeq() == IS_SEQ) { // 有序
            LocalSeqIdBO localSeqIdBO = new LocalSeqIdBO();
            AtomicLong currentValue = new AtomicLong(currentNum);
            localSeqIdBO.setId(idBuilderPO.getId());
            localSeqIdBO.setCurrentValue(currentValue);
            localSeqIdBO.setCurrentStart(currentStart);
            localSeqIdBO.setNextThreshold(nextThreshold);
            localSeqIdBOMap.put(localSeqIdBO.getId(), localSeqIdBO);
        } else {
            LocalUnSeqIdBO localUnSeqIdBO = new LocalUnSeqIdBO();
            localUnSeqIdBO.setId(idBuilderPO.getId());
            localUnSeqIdBO.setCurrentStart(currentStart);
            localUnSeqIdBO.setNextThreshold(nextThreshold);
            Long start = localUnSeqIdBO.getCurrentStart();
            Long end = localUnSeqIdBO.getNextThreshold();
            List<Long> idList = new ArrayList<>();
            // 直接把所有的Id都收集起来到一个数组中
            for (long i = start; i < end; i++) {
                idList.add(i);
            }
            // 将顺序打乱
            Collections.shuffle(idList);
            ConcurrentLinkedQueue<Long> idQueue = new ConcurrentLinkedQueue<>();
            idQueue.addAll(idList);
            localUnSeqIdBO.setIdQueue(idQueue);
            localUnSeqIdBOMap.put(localUnSeqIdBO.getId(), localUnSeqIdBO);
        }
    }

}
