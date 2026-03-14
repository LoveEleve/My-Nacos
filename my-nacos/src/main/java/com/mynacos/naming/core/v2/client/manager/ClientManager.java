/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2.client.manager;

import com.mynacos.naming.core.v2.client.Client;

import java.util.Collection;

/**
 * Client 管理器接口
 *
 * 对照源码: com.alibaba.nacos.naming.core.v2.client.manager.ClientManager
 */
public interface ClientManager {

    /**
     * 新客户端连接
     *
     * @param clientId 客户端ID
     * @return 是否添加成功
     */
    boolean clientConnected(String clientId);

    /**
     * 新客户端连接（直接传入Client对象）
     */
    boolean clientConnected(Client client);

    /**
     * 客户端断开连接
     *
     * @param clientId 客户端ID
     * @return 是否移除成功
     */
    boolean clientDisconnected(String clientId);

    /**
     * 根据ID获取Client
     */
    Client getClient(String clientId);

    /**
     * 判断是否包含某个Client
     */
    boolean contains(String clientId);

    /**
     * 获取所有客户端ID
     */
    Collection<String> allClientId();

    /**
     * 获取客户端总数
     */
    int currentClientCount();

    // TODO: syncClientConnected - 集群同步用
    // TODO: isResponsibleClient - 集群责任判断用
    // TODO: verifyClient - 集群客户端验证用
}
