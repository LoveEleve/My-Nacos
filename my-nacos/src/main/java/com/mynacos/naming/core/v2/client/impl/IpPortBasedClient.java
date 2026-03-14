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
 * IpPortBasedClient - 基于 IP:Port 的客户端实现
 * 
 * 场景：最常见的客户端类型
 * - 服务启动时注册自己
 * - 通过 IP + Port 唯一标识
 * - 支持临时实例（默认）和持久实例
 * 
 * 存储结构：
 * Map<Service, InstancePublishInfo>
 * Key：服务（如 order-service）
 * Value：实例信息（IP、端口、权重等）
 * 
 * 对照：com.alibaba.nacos.naming.core.v2.client.impl.IpPortBasedClient
 */
public class IpPortBasedClient implements Client {

    /**
     * 客户端 ID
     */
    private final String clientId;

    /**
     * 是否为临时客户端
     */
    private final boolean ephemeral;

    /**
     * 最后更新时间 - 用于健康检查
     */
    private volatile long lastUpdatedTime;

    /**
     * 发布的服务实例
     * Key：Service
     * Value：实例发布信息
     */
    private final ConcurrentHashMap<Service, InstancePublishInfo> serviceInstances;

    public IpPortBasedClient(String clientId, boolean ephemeral) {
        this.clientId = clientId;
        this.ephemeral = ephemeral;
        this.lastUpdatedTime = System.currentTimeMillis();
        this.serviceInstances = new ConcurrentHashMap<>();
    }

    /**
     * 工厂方法：创建临时客户端
     */
    public static IpPortBasedClient createEphemeralClient(String ip, int port) {
        String clientId = ip + ":" + port + "#" + true;
        return new IpPortBasedClient(clientId, true);
    }

    /**
     * 工厂方法：创建持久客户端
     */
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
        // 刷新更新时间
        setLastUpdatedTime();
        
        // 增加服务版本号
        service.incrementRevision();
        service.renewUpdateTime();

        // 存储实例信息
        serviceInstances.put(service, instancePublishInfo);

        System.out.println("[Client] Register: " + service.getName() 
            + " -> " + instancePublishInfo.toInetAddr()
            + " (client: " + clientId + ")");
        return true;
    }

    @Override
    public InstancePublishInfo removeServiceInstance(Service service) {
        setLastUpdatedTime();
        service.incrementRevision();
        service.renewUpdateTime();

        InstancePublishInfo removed = serviceInstances.remove(service);
        if (removed != null) {
            System.out.println("[Client] Deregister: " + service.getName()
                + " (client: " + clientId + ")");
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
        serviceInstances.clear();
        System.out.println("[Client] Released: " + clientId);
    }

    // ==================== 额外方法 ====================

    public Map<Service, InstancePublishInfo> getServiceInstances() {
        return serviceInstances;
    }

    @Override
    public String toString() {
        return "IpPortBasedClient{clientId='" + clientId + '\'' 
            + ", instances=" + serviceInstances.size() + '}';
    }
}
