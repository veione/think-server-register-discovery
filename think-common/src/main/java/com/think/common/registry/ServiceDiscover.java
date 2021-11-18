package com.think.common.registry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.curator.x.discovery.strategies.RandomStrategy;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 服务发现
 *
 * @author veione
 * @version 1.0
 * @date 2021/11/18
 */
public class ServiceDiscover {
    private ServiceDiscovery<ServicePayload> serviceDiscovery;
    private final ConcurrentMap<String, ServiceProvider<ServicePayload>> serviceProviderMap = new ConcurrentHashMap<>();

    public ServiceDiscover(CuratorFramework client, String basePath) {
        serviceDiscovery = ServiceDiscoveryBuilder.builder(ServicePayload.class)
                .client(client)
                .basePath(basePath)
                .serializer(new JsonInstanceSerializer<>(ServicePayload.class))
                .build();
    }

    /**
     * Note: When using Curator 2.x (Zookeeper 3.4.x) it's essential that service provider objects are cached by your application and reused.
     * Since the internal NamespaceWatcher objects added by the service provider cannot be removed in Zookeeper 3.4.x,
     * creating a fresh service provider for each call to the same service will eventually exhaust the memory of the JVM.
     */
    public ServiceInstance<ServicePayload> getServiceProvider(String serviceName) throws Exception {
        ServiceProvider<ServicePayload> provider = serviceProviderMap.get(serviceName);
        if (provider == null) {
            provider = serviceDiscovery.serviceProviderBuilder().
                    serviceName(serviceName).
                    providerStrategy(new RandomStrategy<>())
                    .build();

            ServiceProvider<ServicePayload> oldProvider = serviceProviderMap.putIfAbsent(serviceName, provider);
            if (oldProvider != null) {
                provider = oldProvider;
            } else {
                provider.start();
            }
        }

        return provider.getInstance();
    }

    public void start() throws Exception {
        serviceDiscovery.start();
    }

    public void close() throws IOException {
        for (Map.Entry<String, ServiceProvider<ServicePayload>> me : serviceProviderMap.entrySet()) {
            try {
                me.getValue().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        serviceDiscovery.close();
    }

    public ServiceCache<ServicePayload> newServiceCache(String serviceName) throws Exception {
        ServiceCache<ServicePayload> serviceCache = serviceDiscovery.serviceCacheBuilder().name(serviceName).build();
        serviceCache.start();
        return serviceCache;
    }
}
