/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2.client;

import com.mynacos.naming.core.v2.pojo.InstancePublishInfo;
import com.mynacos.naming.core.v2.pojo.Service;

import java.util.Collection;

/**
 * Client - 客户端抽象
 * 
 * 问题：服务实例信息存在哪里？
 * 
 * 传统思路（Nacos 1.x）：
 * - Service 包含 Instance 列表
 * - 问题：一个服务有多个实例，实例从哪里来不清晰
 * 
 * Nacos 2.x 思路：
 * - Client 包含自己发布的实例
 * - 一个 Client（一个应用进程）可以发布多个 Service
 * - 优势：
 *   1. 连接断开时，自动清理该 Client 的所有实例
 *   2. 支持一个进程注册多个服务
 *   3. 与连接管理天然对应
 * 
 * 对照：com.alibaba.nacos.naming.core.v2.client.Client
 */
public interface Client {

    /**
     * 客户端唯一 ID
     * 格式：ip:port#ephemeral
     * 示例：192.168.1.100:8080#true
     */
    String getClientId();

    /**
     * 是否为临时客户端
     * true=临时实例，false=持久实例
     */
    boolean isEphemeral();

    /**
     * 刷新最后更新时间
     */
    void setLastUpdatedTime();

    long getLastUpdatedTime();

    // ==================== 服务发布 ====================

    /**
     * 发布服务实例
     * @param service 服务
     * @param instancePublishInfo 实例信息
     * @return 是否成功
     */
    boolean addServiceInstance(Service service, InstancePublishInfo instancePublishInfo);

    /**
     * 移除服务实例
     */
    InstancePublishInfo removeServiceInstance(Service service);

    /**
     * 获取某服务的实例信息
     */
    InstancePublishInfo getInstancePublishInfo(Service service);

    /**
     * 获取该客户端发布的所有服务
     */
    Collection<Service> getAllPublishedService();

    /**
     * 是否包含某服务
     */
    boolean containsService(Service service);

    // ==================== 资源释放 ====================

    void release();
}
