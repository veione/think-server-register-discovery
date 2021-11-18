package com.think.gate;

import com.think.gate.config.GateConfig;
import com.think.common.config.ServiceConfig;
import com.think.gate.tcp.TcpServer;
import com.think.gate.tcp.client.ClientChannelFactory;
import com.think.gate.tcp.logic.LogicServerChannelFactory;
import com.think.net.IServer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * 网关启动器
 *
 * @author veione
 */
public class GateApplication {
    private static IServer clientServer;
    private static IServer logicServer;
    private static ServiceDiscovery clientServiceDiscovery;
    private static ServiceDiscovery serverServiceDiscovery;

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
                serverServiceDiscovery.close();
                clientServiceDiscovery.close();
                clientServer.stopServer();
                logicServer.stopServer();
                System.out.println("关闭服务：：：：");
            } catch (IOException e) {
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

        ServiceInstance<ServiceConfig> clientService = ServiceInstance.<ServiceConfig>builder()
                .id(String.valueOf(GateConfig.id))
                .registrationTimeUTC(System.currentTimeMillis())
                .name("client")
                .address(GateConfig.privateIp)
                .payload(new ServiceConfig())
                .port(GateConfig.serverPort)
                .uriSpec(new UriSpec("{address}:{port}"))
                .build();
        ServiceInstance<ServiceConfig> serverService = ServiceInstance.<ServiceConfig>builder()
                .id(String.valueOf(GateConfig.id))
                .registrationTimeUTC(System.currentTimeMillis())
                .name("server")
                .address(GateConfig.privateIp)
                .payload(new ServiceConfig())
                .port(GateConfig.clientPort)
                .uriSpec(new UriSpec("{address}:{port}"))
                .build();

        JsonInstanceSerializer<ServiceConfig> serializer = new JsonInstanceSerializer<>(ServiceConfig.class);
        clientServiceDiscovery = ServiceDiscoveryBuilder.builder(ServiceConfig.class)
                .client(client)
                .basePath("think/gate")
                .serializer(serializer)
                .thisInstance(clientService)
                .build();
        clientServiceDiscovery.start();

        serverServiceDiscovery = ServiceDiscoveryBuilder.builder(ServiceConfig.class)
                .client(client)
                .basePath("think/gate")
                .serializer(serializer)
                .thisInstance(serverService)
                .build();
        serverServiceDiscovery.start();
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
