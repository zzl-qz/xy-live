package org.xy.live.im.router.provider.cluster;

import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;


// 实现集群节点选择的时候按照特定的节点进行选择
public class ImRouterClusterInvoker<T> extends AbstractClusterInvoker<T> {


    private final Logger LOGGER = LoggerFactory.getLogger(ImRouterClusterInvoker.class);

    // 构造函数
    public ImRouterClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    protected Result doInvoke(Invocation invocation, List list, LoadBalance loadbalance) throws RpcException {
        // 调用父类方法进行简单的校验
        checkWhetherDestroyed();

        // 从上下文中拿到当前请求
        String ip = (String) RpcContext.getContext().get("ip");
        if (StringUtils.isEmpty(ip)) {
            throw new RuntimeException("ip can not be null!");
        }

        // 获取到指定的rpc服务提供者的所有地址信息
        List<Invoker<T>> invokers = list(invocation); // 获取所有，也就是在Directory上找到所有注册的节点
        LOGGER.info("[invokers] is {}", invokers);

        Invoker<T> matchInvoker = invokers.stream().filter(invoker -> {
            // 拿到我们服务提供者的暴露地址（ip:端口 的格式）
            String serverIp = invoker.getUrl().getHost() + ":" + invoker.getUrl().getPort();
            return serverIp.equals(ip);
        }).findFirst().orElse(null);

        if (matchInvoker == null) {
            throw new RuntimeException("ip is invalid");
        }

        // 向该服务提供者发送实际的 RPC 请求，并等待响应
        return matchInvoker.invoke(invocation);
    }
}