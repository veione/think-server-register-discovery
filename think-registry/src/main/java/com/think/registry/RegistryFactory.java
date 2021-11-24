package com.think.registry;

/**
 * 注册工厂
 *
 * @author veione
 */
public class RegistryFactory {
    private static volatile RegistryService registryService;

    public static RegistryService getInstance(String registryAddr, RegistryType type) throws Exception {
        if (registryService == null) {
            synchronized (RegistryFactory.class) {
                if (registryService == null) {
                    switch (type) {
                        case ZOOKEEPER:
                            registryService = new ZookeeperRegistryService(registryAddr);
                            break;
                    }
                }
            }
        }
        return registryService;
    }
}
