package com.think.common.registry;

/**
 * 服务数据载体
 *
 * @author veione
 * @version 1.0
 * @date 2021/11/18
 */
public class ServicePayload {
    private String cluster;
    private int payload;

    public ServicePayload() {

    }

    public ServicePayload(String cluster, int payload) {
        this.cluster = cluster;
        this.payload = payload;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public int getPayload() {
        return payload;
    }

    public void setPayload(int payload) {
        this.payload = payload;
    }
}
