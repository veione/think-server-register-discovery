package com.think.logic;

import com.think.common.config.ServiceConfig;
import com.think.logic.config.LogicConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 逻辑服启动器
 *
 * @author veione
 */
public class LogicApplication {

    public static void main(String[] args) {
        initConfig();

        CuratorFramework client = CuratorFrameworkFactory.newClient(LogicConfig.zookeeper, new ExponentialBackoffRetry(3000, 3));
        try {
            client.start();
            JsonInstanceSerializer<ServiceConfig> serializer = new JsonInstanceSerializer<>(ServiceConfig.class);
            ServiceDiscovery serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceConfig.class)
                    .client(client)
                    .basePath("think/gate")
                    .serializer(serializer)
                    .thisInstance(null)
                    .build();

            serviceDiscovery.start();

            ServiceCache<ServiceConfig> serverServiceCache = serviceDiscovery.serviceCacheBuilder().name("server").build();
            serverServiceCache.addListener(new ServiceCacheListener() {
                @Override
                public void cacheChanged() {
                    System.out.println("gate service change " + serverServiceCache.getInstances().size());
                    serverServiceCache.getInstances().forEach(it -> {
                        System.out.println("now gate:" + it.getId() + " " + it.getAddress() + "" + it.getPort());
                    });
                    updateGateServer(serverServiceCache.getInstances());
                }

                @Override
                public void stateChanged(CuratorFramework client, ConnectionState newState) {

                }
            });
            serverServiceCache.start();

            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateGateServer(List<ServiceInstance<ServiceConfig>> instances) {
        System.out.println("网关服务器列表：" + instances);
        // 连接到网关服务器
    }

    /**
     * 初始化配置
     */
    private static void initConfig() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = LogicApplication.class.getClassLoader().getResourceAsStream("logic.yaml")) {
            Map<String, Object> obj = yaml.load(inputStream);
            Map<String, Object> logic = (Map<String, Object>) obj.get("logic");
            Map<String, Object> global = (Map<String, Object>) obj.get("global");

            LogicConfig.id = Integer.parseInt(logic.get("id").toString());
            LogicConfig.zookeeper = (String) global.get("zookeeper");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
