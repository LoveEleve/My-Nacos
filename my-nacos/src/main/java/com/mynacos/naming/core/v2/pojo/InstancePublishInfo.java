/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2.pojo;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InstancePublishInfo - 实例发布信息
 * 
 * 存储在 Client 中的实例信息
 * 
 * 对照：com.alibaba.nacos.naming.core.v2.pojo.InstancePublishInfo
 */
public class InstancePublishInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ip;
    private int port;
    private double weight = 1.0;
    private boolean healthy = true;
    private boolean enabled = true;
    private Map<String, String> metadata = new ConcurrentHashMap<>();

    public InstancePublishInfo() {}

    public InstancePublishInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    // Getters & Setters
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    public boolean isHealthy() { return healthy; }
    public void setHealthy(boolean healthy) { this.healthy = healthy; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    /**
     * 生成唯一标识：ip:port
     */
    public String toInetAddr() {
        return ip + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InstancePublishInfo)) return false;
        InstancePublishInfo that = (InstancePublishInfo) o;
        return port == that.port && Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    @Override
    public String toString() {
        return "InstancePublishInfo{" + "ip='" + ip + '\'' 
            + ", port=" + port + ", weight=" + weight 
            + ", healthy=" + healthy + '}';
    }
}
