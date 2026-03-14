/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2;

import com.mynacos.naming.core.v2.pojo.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Nacos service manager for v2.
 *
 * 对照源码: com.alibaba.nacos.naming.core.v2.ServiceManager
 *
 * 设计要点:
 * 1. 单例模式 - 整个 JVM 只有一个实例
 * 2. singletonRepository - Service 作为 key 和 value（单例存储）
 * 3. namespaceSingletonMaps - 按命名空间索引服务
 * 4. 使用 ConcurrentHashMap 保证线程安全
 */
public class ServiceManager {

    /**
     * 单例实例
     */
    private static final ServiceManager INSTANCE = new ServiceManager();

    /**
     * 核心: Service 单例存储
     * Key: Service (通过 equals/hashCode 比较)
     * Value: 同一个 Service 对象
     *
     * 为什么要这样设计?
     * - 保证内存中只有一个 Service 实例
     * - 所有对该服务的引用都指向同一对象
     * - 避免数据不一致
     */
    private final ConcurrentHashMap<Service, Service> singletonRepository;

    /**
     * 按命名空间索引的服务集合
     * 用于快速获取某个命名空间下的所有服务
     */
    private final ConcurrentHashMap<String, Set<Service>> namespaceSingletonMaps;

    /**
     * 私有构造器 - 单例模式
     */
    private ServiceManager() {
        // 初始容量: 1 << 10 = 1024
        this.singletonRepository = new ConcurrentHashMap<>(1 << 10);
        // 初始容量: 1 << 2 = 4
        this.namespaceSingletonMaps = new ConcurrentHashMap<>(1 << 2);
    }

    public static ServiceManager getInstance() {
        return INSTANCE;
    }

    /**
     * 获取单例 Service
     *
     * 核心逻辑:
     * 1. 如果 Service 已存在,返回已存在的实例
     * 2. 如果不存在,创建新的并放入 repository
     * 3. 同时更新 namespace 索引
     *
     * 对照源码第 61-70 行
     */
    public Service getSingleton(Service service) {
        // computeIfAbsent: 如果不存在,则计算并放入
        singletonRepository.computeIfAbsent(service, key -> {
            // TODO: 发布 MetadataEvent.ServiceMetadataEvent
            // NotifyCenter.publishEvent(new MetadataEvent.ServiceMetadataEvent(service, false));
            System.out.println("[ServiceManager] New service created: " + key.getName());
            return key;
        });

        // 获取结果(可能是最新放入的,也可能是已存在的)
        Service result = singletonRepository.get(service);

        // 更新 namespace 索引
        namespaceSingletonMaps.computeIfAbsent(
            result.getNamespace(),
            namespace -> new ConcurrentSkipListSet<>((s1, s2) -> {
                // 按服务名排序
                int cmp = s1.getGroup().compareTo(s2.getGroup());
                if (cmp != 0) return cmp;
                return s1.getName().compareTo(s2.getName());
            })
        );
        namespaceSingletonMaps.get(result.getNamespace()).add(result);

        return result;
    }

    /**
     * 根据 namespace、group、name 获取 Service(如果不存在返回空)
     *
     * 对照源码第 80-82 行
     */
    public Optional<Service> getSingletonIfExist(String namespace, String group, String name) {
        return getSingletonIfExist(Service.newService(namespace, group, name));
    }

    /**
     * 根据 Service 模板获取(如果不存在返回空)
     *
     * 对照源码第 90-92 行
     */
    public Optional<Service> getSingletonIfExist(Service service) {
        return Optional.ofNullable(singletonRepository.get(service));
    }

    /**
     * 获取某个命名空间下的所有服务
     */
    public Set<Service> getSingletons(String namespace) {
        return namespaceSingletonMaps.getOrDefault(
            namespace,
            new HashSet<>(1)
        );
    }

    /**
     * 获取所有命名空间
     */
    public Set<String> getAllNamespaces() {
        return namespaceSingletonMaps.keySet();
    }

    /**
     * 移除 Service 单例
     *
     * 对照源码第 104-109 行
     */
    public Service removeSingleton(Service service) {
        // 从 namespace 索引中移除
        if (namespaceSingletonMaps.containsKey(service.getNamespace())) {
            namespaceSingletonMaps.get(service.getNamespace()).remove(service);
        }
        // 从 repository 中移除
        return singletonRepository.remove(service);
    }

    /**
     * 判断是否包含某个 Service
     */
    public boolean containSingleton(Service service) {
        return singletonRepository.containsKey(service);
    }

    /**
     * 获取服务总数
     */
    public int size() {
        return singletonRepository.size();
    }

    /**
     * 获取所有 Service
     */
    public Collection<Service> getAllServices() {
        return singletonRepository.values();
    }
}
