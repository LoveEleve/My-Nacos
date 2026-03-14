/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2.client.impl;

import com.mynacos.naming.core.v2.client.Client;
import com.mynacos.naming.core.v2.pojo.InstancePublishInfo;
import com.mynacos.naming.core.v2.pojo.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 IP + Port 的客户端实现
 *
 * 对照源码: com.alibaba.nacos.naming.core.v2.client.impl.IpPortBasedClient
 *
 * 这是最常见的客户端类型,用于存储临时实例的注册信息。
 */
public class IpPortBasedClient implements Client {

    /**
     * 客户端ID
     * 格式: ip:port#ephemeral
     */
    private final String clientId;

    /**
     * 是否为临时客户端
     */
    private final boolean ephemeral;

    /**
     * 最后更新时间
     */
    private volatile long lastUpdatedTime;

    /**
     * 该客户端发布的服务实例
     * Key: Service, Value: 实例信息
     */
    private final ConcurrentHashMap<Service, InstancePublishInfo> serviceInstances;

    // TODO: 订阅关系存储
    // private final ConcurrentHashMap<Service, Subscriber> subscribers;

    /**
     * 构造器
     */
    public IpPortBasedClient(String clientId, boolean ephemeral) {
        this.clientId = clientId;
        this.ephemeral = ephemeral;
        this.lastUpdatedTime = System.currentTimeMillis();
        this.serviceInstances = new ConcurrentHashMap<>();
    }

    /**
     * 静态工厂方法
     */
    public static IpPortBasedClient createEphemeralClient(String ip, int port) {
        String clientId = ip + ":" + port + "#" + true;
        return new IpPortBasedClient(clientId, true);
    }

    public static IpPortBasedClient createPersistentClient(String ip, int port) {
        String clientId = ip + ":" + port + "#" + false;
        return new IpPortBasedClient(clientId, false);
    }

    // ==================== Client 接口实现 ====================

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public boolean isEphemeral() {
        return ephemeral;
    }

    @Override
    public void setLastUpdatedTime() {
        this.lastUpdatedTime = System.currentTimeMillis();
    }

    @Override
    public long getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    @Override
    public boolean addServiceInstance(Service service, InstancePublishInfo instancePublishInfo) {
        // 更新时间
        setLastUpdatedTime();
        // 增加服务版本号
        service.incrementRevision();
        service.renewUpdateTime();

        // 存储实例信息
        serviceInstances.put(service, instancePublishInfo);

        System.out.println("[Client] Instance added: " + service.getName()
                + " -> " + instancePublishInfo.toInetAddr()
                + " by client: " + clientId);
        return true;
    }

    @Override
    public InstancePublishInfo removeServiceInstance(Service service) {
        setLastUpdatedTime();
        service.incrementRevision();
        service.renewUpdateTime();

        InstancePublishInfo removed = serviceInstances.remove(service);
        if (removed != null) {
            System.out.println("[Client] Instance removed: " + service.getName()
                    + " by client: " + clientId);
        }
        return removed;
    }

    @Override
    public InstancePublishInfo getInstancePublishInfo(Service service) {
        return serviceInstances.get(service);
    }

    @Override
    public Collection<Service> getAllPublishedService() {
        return serviceInstances.keySet();
    }

    @Override
    public boolean containsService(Service service) {
        return serviceInstances.containsKey(service);
    }

    @Override
    public void release() {
        // 清理资源
        serviceInstances.clear();
        System.out.println("[Client] Released: " + clientId);
    }

    // ==================== 额外方法 ====================

    public Map<Service, InstancePublishInfo> getServiceInstances() {
        return serviceInstances;
    }

    @Override
    public String toString() {
        return "IpPortBasedClient{" +
                "clientId='" + clientId + '\'' +
                ", ephemeral=" + ephemeral +
                ", instances=" + serviceInstances.size() +
                '}';
    }
}
