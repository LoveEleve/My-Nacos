/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2;

import com.mynacos.naming.core.v2.pojo.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ServiceManager - 服务管理器
 * 
 * 问题：如何保证内存中同一个服务（namespace+group+name）只有一个实例？
 * 
 * 方案：单例模式 + ConcurrentHashMap
 * - 单例模式：整个 JVM 只有一个 ServiceManager
 * - Map<Service, Service>：Key和Value是同一个对象
 * - computeIfAbsent：原子性获取或创建
 * 
 * 效果：
 * - Service s1 = Service.newService("public", "DEFAULT_GROUP", "order");
 * - Service s2 = Service.newService("public", "DEFAULT_GROUP", "order");
 * - ServiceManager.getSingleton(s1) == ServiceManager.getSingleton(s2) // true
 * 
 * 对照：com.alibaba.nacos.naming.core.v2.ServiceManager
 */
public class ServiceManager {

    /**
     * 单例实例
     */
    private static final ServiceManager INSTANCE = new ServiceManager();

    /**
     * 核心存储：Service 单例仓库
     * Key：Service（通过 equals 比较）
     * Value：同一个 Service 对象
     * 
     * 为什么要用 Service 做 Key？
     * - Service 的 equals 只比较 namespace+group+name
     * - 相同三元组的 Service 对象 equals 相等
     * - 所以天然去重，保证单例
     */
    private final ConcurrentHashMap<Service, Service> singletonRepository;

    /**
     * 按命名空间索引：加速查询某个 namespace 下的所有服务
     */
    private final ConcurrentHashMap<String, Set<Service>> namespaceSingletonMaps;

    private ServiceManager() {
        // 初始容量 1024，避免频繁扩容
        this.singletonRepository = new ConcurrentHashMap<>(1 << 10);
        // 初始容量 4，namespace 数量通常较少
        this.namespaceSingletonMaps = new ConcurrentHashMap<>(1 << 2);
    }

    public static ServiceManager getInstance() {
        return INSTANCE;
    }

    /**
     * 获取或创建 Service 单例
     * 
     * 核心逻辑：
     * 1. 如果 Service 已存在，返回已存在的实例
     * 2. 如果不存在，放入 repository 并返回
     * 3. 同时更新 namespace 索引
     */
    public Service getSingleton(Service service) {
        // computeIfAbsent：原子操作，如果不存在则计算并放入
        singletonRepository.computeIfAbsent(service, key -> {
            System.out.println("[ServiceManager] Create service: " + key.getName());
            return key;
        });

        Service result = singletonRepository.get(service);

        // 更新 namespace 索引
        namespaceSingletonMaps.computeIfAbsent(
            result.getNamespace(),
            namespace -> ConcurrentHashMap.newKeySet()
        );
        namespaceSingletonMaps.get(result.getNamespace()).add(result);

        return result;
    }

    /**
     * 根据参数获取 Service（如果不存在返回空）
     */
    public Optional<Service> getSingletonIfExist(String namespace, String group, String name) {
        return getSingletonIfExist(Service.newService(namespace, group, name));
    }

    public Optional<Service> getSingletonIfExist(Service service) {
        return Optional.ofNullable(singletonRepository.get(service));
    }

    /**
     * 获取某个命名空间下的所有服务
     */
    public Set<Service> getSingletons(String namespace) {
        return namespaceSingletonMaps.getOrDefault(namespace, new HashSet<>(1));
    }

    /**
     * 获取所有命名空间
     */
    public Set<String> getAllNamespaces() {
        return namespaceSingletonMaps.keySet();
    }

    /**
     * 移除 Service
     */
    public Service removeSingleton(Service service) {
        if (namespaceSingletonMaps.containsKey(service.getNamespace())) {
            namespaceSingletonMaps.get(service.getNamespace()).remove(service);
        }
        return singletonRepository.remove(service);
    }

    public boolean containSingleton(Service service) {
        return singletonRepository.containsKey(service);
    }

    public int size() {
        return singletonRepository.size();
    }

    public Collection<Service> getAllServices() {
        return singletonRepository.values();
    }
}
