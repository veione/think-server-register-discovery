package com.think.registry.util;

public class ServiceHelper {
    public static String buildServiceKey(String serviceName, String serviceVersion) {
        return String.join("#", serviceName, serviceVersion);
    }
}
