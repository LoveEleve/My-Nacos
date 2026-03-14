package com.mynacos.server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 服务管理器 - 第2章：服务存在哪？
 * 
 * 问题：服务端怎么存储服务信息？
 * 解决方案：三层 Map 结构
 * namespace -> group -> serviceName -> Service
 */
public class ServiceManager {
    
    /**
     * 三层索引结构：
     * namespace -> group -> serviceName -> Service
     * 
     * 示例：
     * "public" -> "DEFAULT_GROUP" -> "order-service" -> Service
     */
    private final Map<String, Map<String, Map<String, Service>>> serviceMap 
        = new ConcurrentHashMap<>();
    
    /**
     * 添加实例
     */
    public void addInstance(String namespaceId, String groupName, 
                          String serviceName, Instance instance) {
        // 1. 获取或创建 namespace
        Map<String, Map<String, Service>> namespace = serviceMap
            .computeIfAbsent(namespaceId, k -> new ConcurrentHashMap<>());
        
        // 2. 获取或创建 group
        Map<String, Service> group = namespace
            .computeIfAbsent(groupName, k -> new ConcurrentHashMap<>());
        
        // 3. 获取或创建 Service
        Service service = group.computeIfAbsent(serviceName, k -> {
            Service s = new Service();
            s.setName(serviceName);
            return s;
        });
        
        // 4. 添加实例
        service.addInstance(instance);
        service.setLastModifiedTime(System.currentTimeMillis());
    }
    
    /**
     * 获取实例列表
     */
    public List<Instance> getInstances(String namespaceId, 
                                     String groupName, 
                                     String serviceName) {
        // 1. 查找 namespace
        Map<String, Map<String, Service>> namespace = serviceMap.get(namespaceId);
        if (namespace == null) {
            return Collections.emptyList();
        }
        
        // 2. 查找 group
        Map<String, Service> group = namespace.get(groupName);
        if (group == null) {
            return Collections.emptyList();
        }
        
        // 3. 查找 Service
        Service service = group.get(serviceName);
        if (service == null) {
            return Collections.emptyList();
        }
        
        // 4. 返回实例列表
        return service.getInstances();
    }
    
    /**
     * 获取所有服务（用于健康检查）
     */
    public Collection<Service> getAllServices() {
        List<Service> result = new ArrayList<>();
        for (Map<String, Map<String, Service>> namespace : serviceMap.values()) {
            for (Map<String, Service> group : namespace.values()) {
                result.addAll(group.values());
            }
        }
        return result;
    }
}
