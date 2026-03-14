/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2.client.manager;

import com.mynacos.naming.core.v2.client.Client;

import java.util.Collection;

/**
 * ClientManager - 客户端管理器接口
 * 
 * 问题：如何管理大量客户端连接？
 * 
 * 场景：
 * - 服务启动时，建立连接并注册客户端
 * - 服务停止时，断开连接并清理客户端
 * - 需要快速根据 clientId 查找客户端
 * 
 * 方案：接口 + 多种实现
 * - EphemeralIpPortClientManager：管理临时实例（基于 IP:Port）
 * - PersistentIpPortClientManager：管理持久实例
 * - ConnectionBasedClientManager：管理基于长连接的客户端（Nacos 2.x 默认）
 * 
 * 对照：com.alibaba.nacos.naming.core.v2.client.manager.ClientManager
 */
public interface ClientManager {

    /**
     * 客户端连接
     * @param clientId 客户端 ID
     * @return 是否成功
     */
    boolean clientConnected(String clientId);

    /**
     * 客户端连接（直接传入 Client 对象）
     */
    boolean clientConnected(Client client);

    /**
     * 客户端断开
     * @param clientId 客户端 ID
     * @return 是否成功
     */
    boolean clientDisconnected(String clientId);

    /**
     * 根据 ID 获取客户端
     */
    Client getClient(String clientId);

    /**
     * 是否包含某客户端
     */
    boolean contains(String clientId);

    /**
     * 获取所有客户端 ID
     */
    Collection<String> allClientId();

    /**
     * 当前客户端数量
     */
    int currentClientCount();
}
