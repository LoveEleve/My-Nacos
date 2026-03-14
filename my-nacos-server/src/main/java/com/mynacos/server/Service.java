package com.mynacos.server;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 服务 - 第2章：服务存在哪？
 * 
 * 问题：一个服务有哪些属性？
 * 核心：实例列表用 CopyOnWriteArrayList（读多写少优化）
 */
public class Service {
    
    private String name;                           // 服务名
    private List<Instance> instances;              // 临时实例列表
    private Set<String> subscriberConnections;     // 订阅者连接ID集合
    private long lastModifiedTime;                 // 最后修改时间
    
    public Service() {
        // 第3章：服务实例列表怎么读？
        // 读多写少场景，用 CopyOnWriteArrayList 实现读无锁
        this.instances = new CopyOnWriteArrayList<>();
        this.subscriberConnections = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * 添加实例
     */
    public void addInstance(Instance instance) {
        // 先移除旧的（如果存在，根据ip+port唯一标识）
        instances.removeIf(i -> i.getIp().equals(instance.getIp()) 
                          && i.getPort() == instance.getPort());
        instances.add(instance);
    }
    
    /**
     * 移除实例
     */
    public void removeInstance(Instance instance) {
        instances.removeIf(i -> i.getIp().equals(instance.getIp()) 
                          && i.getPort() == instance.getPort());
    }
    
    /**
     * 根据IP和端口查找实例
     */
    public Instance findInstance(String ip, int port) {
        for (Instance instance : instances) {
            if (instance.getIp().equals(ip) && instance.getPort() == port) {
                return instance;
            }
        }
        return null;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<Instance> getInstances() {
        return instances;
    }
    
    public Set<String> getSubscriberConnections() {
        return subscriberConnections;
    }
    
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }
    
    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
