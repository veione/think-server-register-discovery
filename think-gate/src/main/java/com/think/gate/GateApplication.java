package com.think.gate;

import com.alibaba.fastjson.JSON;
import com.think.common.constants.ZkNode;
import com.think.common.registry.ServicePayload;
import com.think.common.registry.ServiceRegistry;
import com.think.gate.config.GateConfig;
import com.think.gate.tcp.TcpServer;
import com.think.gate.tcp.client.ClientChannelFactory;
import com.think.gate.tcp.logic.LogicServerChannelFactory;
import com.think.net.IServer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 网关启动器
 *
 * @author veione
 */
public class GateApplication {
    private static IServer clientServer;
    private static IServer logicServer;
    private static ServiceRegistry serviceRegistry;

    public static void main(String[] args) throws Exception {
        // 初始化解析配置文件
        initConfig();

        clientServer = new TcpServer(GateConfig.clientPort, new ClientChannelFactory());
        logicServer = new TcpServer(GateConfig.serverPort, new LogicServerChannelFactory());
        clientServer.startServer();
        logicServer.startServer();

        // 注册服务
        registerService();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serviceRegistry.close();
                clientServer.stopServer();
                logicServer.stopServer();
                System.out.println("关闭服务：：：：");
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

        System.out.println("register service success...");

        TimeUnit.SECONDS.sleep(5);

        Collection<ServiceInstance<ServicePayload>> list = serviceRegistry.queryForInstances(ZkNode.ZK_GATE_CLIENT_SERVICE, "client");
        if (list != null && list.size() > 0) {
            System.out.println("service:" + ZkNode.ZK_GATE_CLIENT_SERVICE + " provider list:" + JSON.toJSONString(list));
        } else {
            System.out.println("service:" + ZkNode.ZK_GATE_SERVER_SERVICE + " provider is empty...");
        }
        list = serviceRegistry.queryForInstances(ZkNode.ZK_GATE_SERVER_SERVICE, "server");
        if (list != null && list.size() > 0) {
            System.out.println("service:" + ZkNode.ZK_GATE_SERVER_SERVICE + " provider list:" + JSON.toJSONString(list));
        } else {
            System.out.println("service:" + ZkNode.ZK_GATE_SERVER_SERVICE + " provider is empty...");
        }
    }

    /**
     * 初始化解析配置
     *
     * @throws IOException
     */
    private static void initConfig() throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = GateApplication.class.getClassLoader().getResourceAsStream("gate.yaml")) {
            Map<String, Object> obj = yaml.load(inputStream);
            Map<String, Object> gate = (Map<String, Object>) obj.get("gate");
            Map<String, Object> global = (Map<String, Object>) obj.get("global");

            GateConfig.id = Integer.parseInt(gate.get("id").toString());
            GateConfig.privateIp = gate.get("privateIp").toString();
            GateConfig.clientPort = Integer.parseInt(gate.get("clientPort").toString());
            GateConfig.serverPort = Integer.parseInt(gate.get("serverPort").toString());
            GateConfig.zookeeper = global.get("zookeeper").toString();
        }
    }
}
