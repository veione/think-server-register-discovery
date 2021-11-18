package com.think.common.registry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 服务注册
 *
 * @author veione
 * @version 1.0
 * @date 2021/11/18
 */
public class ServiceRegistry {
    private final String basePath;
    private final CuratorFramework client;
    private final ConcurrentMap<String, ServiceDiscovery<ServicePayload>> serviceDiscoveryMap = new ConcurrentHashMap<>();
    private final InstanceSerializer<ServicePayload> serializer;

    public ServiceRegistry(CuratorFramework client, String basePath) {
        this.client = client;
        this.basePath = basePath;
        this.serializer = new JsonInstanceSerializer<>(ServicePayload.class);
    }

    public void registerService(String serviceName, ServiceInstance<ServicePayload> instance) throws Exception {
        ServiceDiscovery<ServicePayload> serviceDiscovery = ServiceDiscoveryBuilder.builder(ServicePayload.class)
                .client(client)
                .basePath(this.basePath)
                .serializer(this.serializer)
                .thisInstance(instance)
                .build();
        ServiceDiscovery<ServicePayload> oldServiceDiscovery = this.serviceDiscoveryMap.putIfAbsent(serviceName, serviceDiscovery);
        if (oldServiceDiscovery != null) {
            serviceDiscovery = oldServiceDiscovery;
        }
        serviceDiscovery.start();
    }

    public void updateService(String serviceName, ServiceInstance<ServicePayload> instance) throws Exception {
        ServiceDiscovery<ServicePayload> serviceDiscovery = this.serviceDiscoveryMap.get(serviceName);
        if (serviceDiscovery != null) {
            serviceDiscovery.updateService(instance);
        }
    }

    public void unregisterService(String serviceName, ServiceInstance<ServicePayload> instance) throws Exception {
        ServiceDiscovery<ServicePayload> serviceDiscovery = this.serviceDiscoveryMap.get(serviceName);
        if (serviceDiscovery != null) {
            serviceDiscovery.unregisterService(instance);
        }
    }

    public Collection<ServiceInstance<ServicePayload>> queryForInstances(String serviceName, String name) throws Exception {
        ServiceDiscovery<ServicePayload> serviceDiscovery = this.serviceDiscoveryMap.get(serviceName);
        if (serviceDiscovery != null) {
            return serviceDiscovery.queryForInstances(name);
        }
        return Collections.emptyList();
    }

    public ServiceInstance<ServicePayload> queryForInstance(String serviceName, String name, String id) throws Exception {
        ServiceDiscovery<ServicePayload> serviceDiscovery = this.serviceDiscoveryMap.get(serviceName);
        if (serviceDiscovery != null) {
            return serviceDiscovery.queryForInstance(name, id);
        }
        return null;
    }

    public void close() throws Exception {
        for (Map.Entry<String, ServiceDiscovery<ServicePayload>> me : serviceDiscoveryMap.entrySet()) {
            try {
                me.getValue().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
