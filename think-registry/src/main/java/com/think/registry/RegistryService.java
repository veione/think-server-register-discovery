package com.think.registry;

import com.think.common.registry.ServiceMeta;

import java.io.IOException;

/**
 * 注册服务
 *
 * @author veione
 */
public interface RegistryService {

    void register(ServiceMeta serviceMeta) throws Exception;

    void unRegister(ServiceMeta serviceMeta) throws Exception;

    ServiceMeta discovery(String serviceName, int invokerHashCode) throws Exception;

    void destroy() throws IOException;
}
