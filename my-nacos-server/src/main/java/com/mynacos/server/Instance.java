package com.mynacos.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务实例 - 第2章：服务存在哪？
 * 
 * 问题：实例有哪些属性？
 */
public class Instance {
    
    private String instanceId;      // 实例ID
    private String ip;              // IP地址
    private int port;               // 端口
    private double weight = 1.0;    // 权重（默认1.0）
    private boolean healthy = true; // 健康状态（默认健康）
    private boolean ephemeral = true; // 是否临时实例（默认临时）
    private Map<String, String> metadata = new ConcurrentHashMap<>();  // 元数据
    private long lastBeatTime;      // 最后心跳时间
    
    public Instance() {}
    
    public Instance(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.instanceId = ip + "#" + port;
        this.lastBeatTime = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getInstanceId() {
        return instanceId;
    }
    
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
    
    public String getIp() {
        return ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public double getWeight() {
        return weight;
    }
    
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    public boolean isHealthy() {
        return healthy;
    }
    
    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }
    
    public boolean isEphemeral() {
        return ephemeral;
    }
    
    public void setEphemeral(boolean ephemeral) {
        this.ephemeral = ephemeral;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public long getLastBeatTime() {
        return lastBeatTime;
    }
    
    public void setLastBeatTime(long lastBeatTime) {
        this.lastBeatTime = lastBeatTime;
    }
    
    @Override
    public String toString() {
        return "Instance{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", healthy=" + healthy +
                '}';
    }
}
