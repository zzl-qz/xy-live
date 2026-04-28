//package org.xy.live.gitf.provider.job;
//
//import com.xxl.job.core.context.XxlJobHelper;
//import com.xxl.job.core.handler.annotation.XxlJob;
//import jakarta.annotation.Resource;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//import org.xy.live.gitf.provider.service.IShopCarService;
//
///**
// * 购物车定时任务
// */
//@Component
//public class ShopCartJob {
//
//    @Resource
//    private IShopCarService shopCarService;
//
//    private final static Logger LOGGER = LoggerFactory.getLogger(ShopCartJob.class);
//
//    /**
//     * 删除冷用户购物车缓存数据
//     * @return
//     */
//    @XxlJob("deleteColdUserShopCartCache")
//    public void deleteColdUserShopCartCache() {
//        try {
//            shopCarService.removeColdUserCartInCache();
//            XxlJobHelper.log("冷用户购物车缓存清理完成");
//        } catch (Exception e) {
//            LOGGER.error("请求冷用户购物车缓存数据失败，失败原因", e);
//            XxlJobHelper.log("冷用户购物车缓存清理失败：" + e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
//
//}
