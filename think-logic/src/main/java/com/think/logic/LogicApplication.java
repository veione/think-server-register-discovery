package com.think.logic;

import com.think.common.constants.ZkNode;
import com.think.common.registry.ServiceDiscover;
import com.think.common.registry.ServicePayload;
import com.think.logic.config.LogicConfig;
import com.think.net.User;
import com.think.net.client.connector.DefaultCommonClientConnector;
import com.think.net.common.Message;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.think.net.common.NettyCommonProtocol.REQUEST;

/**
 * 逻辑服启动器
 *
 * @author veione
 */
public class LogicApplication {
    private static final Logger logger = LoggerFactory.getLogger(LogicApplication.class);
    private static ServiceDiscover serviceDiscover;
    private static final ConcurrentMap<String, DefaultCommonClientConnector> connectorMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        initConfig();

        CuratorFramework client = CuratorFrameworkFactory.newClient(LogicConfig.zookeeper, new ExponentialBackoffRetry(3000, 3));
        try {
            client.start();
            serviceDiscover = new ServiceDiscover(client, ZkNode.ZK_GATE_BASE_PATH);
            serviceDiscover.start();

            ServiceCache<ServicePayload> serviceCache = serviceDiscover.newServiceCache("server");
            serviceCache.addListener(new ServiceCacheListener() {
                @Override
                public void cacheChanged() {
                   logger.warn("gate service change {}", serviceCache.getInstances().size());
                    serviceCache.getInstances().forEach(it -> {
                        logger.warn("now gate: {} - {} - {}", it.getId(), it.getAddress(), it.getPort());
                    });
                    updateGateServer(serviceCache.getInstances());
                }

                @Override
                public void stateChanged(CuratorFramework client, ConnectionState newState) {

                }
            });
            updateGateServer(serviceCache.getInstances());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    serviceDiscover.close();
                    for (DefaultCommonClientConnector connector : connectorMap.values()) {
                        connector.shutdownGracefully();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateGateServer(List<ServiceInstance<ServicePayload>> instances) {
        logger.info("网关服务器列表：{}", instances);
        // 连接到网关服务器
        for (ServiceInstance<ServicePayload> instance : instances) {
            DefaultCommonClientConnector connector = connectorMap.get(instance.getId());
            if (connector == null) {
                connector = new DefaultCommonClientConnector();
                Channel channel = connector.connect(instance.getPort(), instance.getAddress());
                User user = new User(1, "dubbo");
                Message message = new Message();
                message.sign(REQUEST);
                message.data(user);
                //获取到channel发送双方规定的message格式的信息
                channel.writeAndFlush(message).addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            logger.info("send fail,reason is {}", future.cause().getMessage());
                        }
                    }
                });
                //防止对象处理发生异常的情况
                DefaultCommonClientConnector.MessageNonAck msgNonAck = new DefaultCommonClientConnector.MessageNonAck(message, channel);
                connector.addNeedAckMessageInfo(msgNonAck);

                connectorMap.putIfAbsent(instance.getId(), connector);
            }
        }
    }

    /**
     * 初始化配置
     */
    private static void initConfig() {
        Config config = ConfigFactory.load("logic.conf");
        LogicConfig.id = config.getInt("logic.id");
        LogicConfig.zookeeper = config.getString("logic.zookeeper");
    }
}
