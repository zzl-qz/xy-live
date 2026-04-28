package org.xy.live.im.core.server.starter;

import io.netty.channel.ChannelInitializer;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.xy.live.im.core.server.common.ChannelHandlerContextCache;
import org.xy.live.im.core.server.common.ImMsgDecoder;
import org.xy.live.im.core.server.common.ImMsgEncoder;
import org.xy.live.im.core.server.handler.tcp.TcpImServerCoreHandler;

@Configuration
@RefreshScope
public class NettyImServerStarter implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyImServerStarter.class);

    //要监听的端口
    @Value("${xy.im.port}")
    private int port;
    @Resource
    private TcpImServerCoreHandler imServerCoreHandler;

    @Resource
    private Environment environment;

    //基于Netty去启动一个java进程，绑定监听的端口
    public void startApplication() throws InterruptedException {
        //处理accept事件
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        //处理read&write事件
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        //netty初始化相关的handler
        bootstrap.childHandler(new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                //打印日志，方便观察
                LOGGER.info("初始化连接渠道");
                //设计消息体ImMsg
                //添加编解码器
                channel.pipeline().addLast(new ImMsgEncoder());
                channel.pipeline().addLast(new ImMsgDecoder());
                //设置这个netty处理handler
                channel.pipeline().addLast(imServerCoreHandler);
            }
        });
        //基于JVM的钩子函数去实现优雅关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }));

        //将启动时im服务器的ip和端口记录下来，用于Router模块转发时使用
        //添加启动参数：
        // -DDUBBO_IP_TO_REGISTRY=192.168.31.222  (启动服务的机器的ip地址)
        // -DDUBBO_PORT_TO_REGISTRY=9095
        //注意VM参数添加的是-D参数，前面是两个D，后面获取property时只有一个D
        // String registryIp = InetAddress.getLocalHost().getHostAddress();
        // String registryIp = environment.getProperty("DUBBO_IP_TO_REGISTRY");//部署时我们使用这条语句获取ip地址
        String registryIp = "192.168.116.1";//部署时我们使用这条语句获取ip地址
        // String registryPort = environment.getProperty("DUBBO_PORT_TO_REGISTRY");
        String registryPort = "9095";
        System.out.println(registryIp + ":" + registryPort);
        if(StringUtils.isEmpty(registryIp) || StringUtils.isEmpty(registryPort)) {
            throw new IllegalArgumentException("启动参数中的注册端口和注册ip不能为空");
        }
        ChannelHandlerContextCache.setServerIpAddress(registryIp + ":" + registryPort);
        LOGGER.info("Netty服务启动成功，机器启动ip和dubbo服务端口为{}", registryIp + ":" + registryPort);

        ChannelFuture channelFuture = bootstrap.bind(port).sync();
        LOGGER.info("Netty服务启动成功，监听端口为{}", port);
        //这里会阻塞主线程，实现服务长期开启的效果
        channelFuture.channel().closeFuture().sync();
    }


    // 初始化加载bean的时候也会执行到这里，启动Netty监听
    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startApplication();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "xy-live-im-server").start();
    }
}