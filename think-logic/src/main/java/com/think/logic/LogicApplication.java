package com.think.logic;

import com.think.common.constants.ZkNode;
import com.think.common.registry.ServiceDiscover;
import com.think.common.registry.ServicePayload;
import com.think.logic.config.LogicConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceInstance;
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
    private static ServiceDiscover serviceDiscover;

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
                    System.out.println("gate service change " + serviceCache.getInstances().size());
                    serviceCache.getInstances().forEach(it -> {
                        System.out.println("now gate:" + it.getId() + " " + it.getAddress() + "" + it.getPort());
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
