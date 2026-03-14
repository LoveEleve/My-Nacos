/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2.client;

import com.mynacos.naming.core.v2.pojo.InstancePublishInfo;
import com.mynacos.naming.core.v2.pojo.Service;

import java.util.Collection;

/**
 * Nacos naming client.
 *
 * 对照源码: com.alibaba.nacos.naming.core.v2.client.Client
 *
 * <p>服务端存储的客户端抽象概念。用于存储客户端发布了哪些服务、订阅了哪些服务。
 *
 * 设计要点:
 * 1. Client 是服务注册的入口 - 一个 Client 可以发布多个 Service
 * 2. Client 存储自己的实例信息和服务订阅关系
 * 3. 支持临时实例和持久实例
 */
public interface Client {

    /**
     * 获取客户端唯一ID
     * 格式: {@code ip:port#ephemeral}
     * 例如: {@code 192.168.1.100:8080#true}
     */
    String getClientId();

    /**
     * 是否为临时客户端
     */
    boolean isEphemeral();

    /**
     * 设置最后更新时间为当前时间
     */
    void setLastUpdatedTime();

    /**
     * 获取最后更新时间
     */
    long getLastUpdatedTime();

    // ==================== 服务发布相关 ====================

    /**
     * 添加服务实例
     *
     * @param service 要发布的服务
     * @param instancePublishInfo 实例信息
     * @return 是否添加成功
     */
    boolean addServiceInstance(Service service, InstancePublishInfo instancePublishInfo);

    /**
     * 移除服务实例
     *
     * @param service 服务
     * @return 被移除的实例信息,如果不存在返回 null
     */
    InstancePublishInfo removeServiceInstance(Service service);

    /**
     * 获取服务实例信息
     */
    InstancePublishInfo getInstancePublishInfo(Service service);

    /**
     * 获取该客户端发布的所有服务
     */
    Collection<Service> getAllPublishedService();

    /**
     * 是否包含某个服务的实例
     */
    boolean containsService(Service service);

    // ==================== 服务订阅相关 (占位) ====================

    // TODO: addServiceSubscriber
    // TODO: removeServiceSubscriber
    // TODO: getSubscriber

    // ==================== 连接相关 (占位) ====================

    /**
     * 释放资源
     */
    void release();
}
