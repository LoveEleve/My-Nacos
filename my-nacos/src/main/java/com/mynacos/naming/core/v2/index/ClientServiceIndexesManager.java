/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2.index;

import com.mynacos.naming.core.v2.pojo.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClientServiceIndexesManager - 客户端与服务双向索引
 *
 * 问题：如何快速找到订阅了某服务的所有客户端？
 *
 * 方案：双向索引
 * - Service → Set<clientId> （订阅了该服务的客户端）
 * - clientId → Set<Service> （该客户端订阅了哪些服务）
 *
 * 对照：com.alibaba.nacos.naming.core.v2.index.ClientServiceIndexesManager
 */
public class ClientServiceIndexesManager {

    /**
     * 单例实例
     */
    private static final ClientServiceIndexesManager INSTANCE = new ClientServiceIndexesManager();

    /**
     * 订阅索引：Service → 订阅该服务的客户端集合
     */
    private final Map<Service, Set<String>> subscriberIndexes;

    /**
     * 反向索引：客户端 → 订阅的服务集合
     * 用于客户端断开时清理订阅
     */
    private final Map<String, Set<Service>> subscriberClientIndexes;

    private ClientServiceIndexesManager() {
        this.subscriberIndexes = new ConcurrentHashMap<>();
        this.subscriberClientIndexes = new ConcurrentHashMap<>();
    }

    public static ClientServiceIndexesManager getInstance() {
        return INSTANCE;
    }

    /**
     * 添加订阅关系
     *
     * @param service  服务
     * @param clientId 客户端ID
     */
    public void addSubscriber(Service service, String clientId) {
        // Service → clientId
        subscriberIndexes.computeIfAbsent(service, k -> ConcurrentHashMap.newKeySet()).add(clientId);

        // clientId → Service
        subscriberClientIndexes.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet()).add(service);

        System.out.println("[Indexes] Client " + clientId + " subscribed to " + service.getName());
    }

    /**
     * 移除订阅关系
     *
     * @param service  服务
     * @param clientId 客户端ID
     */
    public void removeSubscriber(Service service, String clientId) {
        // 从 Service → clientId 中移除
        Set<String> clientIds = subscriberIndexes.get(service);
        if (clientIds != null) {
            clientIds.remove(clientId);
            if (clientIds.isEmpty()) {
                subscriberIndexes.remove(service);
            }
        }

        // 从 clientId → Service 中移除
        Set<Service> services = subscriberClientIndexes.get(clientId);
        if (services != null) {
            services.remove(service);
            if (services.isEmpty()) {
                subscriberClientIndexes.remove(clientId);
            }
        }

        System.out.println("[Indexes] Client " + clientId + " unsubscribed from " + service.getName());
    }

    /**
     * 获取订阅了某服务的所有客户端
     *
     * @param service 服务
     * @return 客户端ID集合
     */
    public Set<String> getSubscribers(Service service) {
        return subscriberIndexes.getOrDefault(service, ConcurrentHashMap.newKeySet());
    }

    /**
     * 获取某客户端订阅的所有服务
     *
     * @param clientId 客户端ID
     * @return 服务集合
     */
    public Set<Service> getSubscribedServices(String clientId) {
        return subscriberClientIndexes.getOrDefault(clientId, ConcurrentHashMap.newKeySet());
    }

    /**
     * 客户端断开时，清理所有订阅
     *
     * @param clientId 客户端ID
     */
    public void removeClientAllSubscription(String clientId) {
        Set<Service> services = subscriberClientIndexes.remove(clientId);
        if (services != null) {
            for (Service service : services) {
                Set<String> clientIds = subscriberIndexes.get(service);
                if (clientIds != null) {
                    clientIds.remove(clientId);
                    if (clientIds.isEmpty()) {
                        subscriberIndexes.remove(service);
                    }
                }
            }
            System.out.println("[Indexes] Client " + clientId + " all subscriptions removed");
        }
    }

    /**
     * 获取所有有订阅者的服务
     */
    public Set<Service> getAllSubscribedServices() {
        return subscriberIndexes.keySet();
    }
}
