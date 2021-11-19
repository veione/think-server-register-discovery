package com.think.gate;

import com.alibaba.fastjson.JSON;
import com.think.common.constants.ZkNode;
import com.think.common.registry.ServicePayload;
import com.think.common.registry.ServiceRegistry;
import com.think.gate.config.GateConfig;
import com.think.net.srv.acceptor.DefaultChannelEventListener;
import com.think.net.srv.acceptor.DefaultCommonSrvAcceptor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 网关启动器
 *
 * @author veione
 */
public class GateApplication {
    private static final Logger logger = LoggerFactory.getLogger(GateApplication.class);
    private static DefaultCommonSrvAcceptor clientServer;
    private static DefaultCommonSrvAcceptor gameServer;
    private static ServiceRegistry serviceRegistry;

    public static void main(String[] args) throws Exception {
        // 初始化解析配置文件
        initConfig();

        clientServer = new DefaultCommonSrvAcceptor("GateClient", GateConfig.clientPort, new DefaultChannelEventListener());
        clientServer.start();

        gameServer = new DefaultCommonSrvAcceptor("GateServer", GateConfig.serverPort, new DefaultChannelEventListener());
        gameServer.start();

        // 注册服务
        registerService();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serviceRegistry.close();
                clientServer.shutdownGracefully();
                gameServer.shutdownGracefully();
                logger.warn("关闭服务：：：：");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    /**
     * 注册服务
     */
    private static void registerService() throws Exception {
        CuratorFramework client = CuratorFrameworkFactory.newClient(GateConfig.zookeeper, new ExponentialBackoffRetry(3000, 3));
        client.start();

        serviceRegistry = new ServiceRegistry(client, ZkNode.ZK_GATE_BASE_PATH);

        ServiceInstance<ServicePayload> clientService = ServiceInstance.<ServicePayload>builder()
                .id(String.valueOf(GateConfig.id))
                .registrationTimeUTC(System.currentTimeMillis())
                .name("client")
                .address(GateConfig.privateIp)
                .payload(new ServicePayload("HZ", 1))
                .port(GateConfig.clientPort)
                .uriSpec(new UriSpec("{scheme}://{address}:{port}"))
                .build();

        ServiceInstance<ServicePayload> serverService = ServiceInstance.<ServicePayload>builder()
                .id(String.valueOf(GateConfig.id))
                .registrationTimeUTC(System.currentTimeMillis())
                .name("server")
                .address(GateConfig.privateIp)
                .payload(new ServicePayload("QD", 2))
                .port(GateConfig.serverPort)
                .uriSpec(new UriSpec("{scheme}://{address}:{port}"))
                .build();

        serviceRegistry.registerService(ZkNode.ZK_GATE_CLIENT_SERVICE, clientService);
        serviceRegistry.registerService(ZkNode.ZK_GATE_SERVER_SERVICE, serverService);

        logger.info("register service success...");

        TimeUnit.SECONDS.sleep(5);

        Collection<ServiceInstance<ServicePayload>> list = serviceRegistry.queryForInstances(ZkNode.ZK_GATE_CLIENT_SERVICE, "client");
        if (list != null && list.size() > 0) {
            logger.info("service:" + ZkNode.ZK_GATE_CLIENT_SERVICE + " provider list:" + JSON.toJSONString(list));
        } else {
            logger.warn("service:" + ZkNode.ZK_GATE_SERVER_SERVICE + " provider is empty...");
        }
        list = serviceRegistry.queryForInstances(ZkNode.ZK_GATE_SERVER_SERVICE, "server");
        if (list != null && list.size() > 0) {
            logger.info("service:" + ZkNode.ZK_GATE_SERVER_SERVICE + " provider list:" + JSON.toJSONString(list));
        } else {
            logger.warn("service:" + ZkNode.ZK_GATE_SERVER_SERVICE + " provider is empty...");
        }
    }

    /**
     * 初始化解析配置
     *
     * @throws IOException
     */
    private static void initConfig() throws IOException {
        Config config = ConfigFactory.load("gate.conf");
        GateConfig.id = config.getInt("gate.id");
        GateConfig.privateIp = config.getString("gate.privateIp");
        GateConfig.clientPort = config.getInt("gate.clientPort");
        GateConfig.serverPort = config.getInt("gate.serverPort");
        GateConfig.zookeeper = config.getString("gate.zookeeper");
    }
}
