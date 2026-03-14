/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2.client.manager.impl;

import com.mynacos.naming.core.v2.client.Client;
import com.mynacos.naming.core.v2.client.impl.IpPortBasedClient;
import com.mynacos.naming.core.v2.client.manager.ClientManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EphemeralIpPortClientManager - 临时 IP:Port 客户端管理器
 * 
 * 职责：
 * - 管理临时实例的客户端（ephemeral=true）
 * - 存储 clientId -> Client 的映射
 * - 连接断开时自动清理
 * 
 * 存储：ConcurrentHashMap<String, Client>
 * - Key：clientId（如 192.168.1.100:8080#true）
 * - Value：IpPortBasedClient 对象
 * 
 * 对照：com.alibaba.nacos.naming.core.v2.client.manager.impl.EphemeralIpPortClientManager
 */
public class EphemeralIpPortClientManager implements ClientManager {

    /**
     * 客户端存储
     */
    private final ConcurrentHashMap<String, Client> clients;

    public EphemeralIpPortClientManager() {
        this.clients = new ConcurrentHashMap<>();
    }

    @Override
    public boolean clientConnected(String clientId) {
        // 解析 clientId
        String[] parts = clientId.split("#");
        if (parts.length != 2) {
            return false;
        }

        String[] ipPort = parts[0].split(":");
        if (ipPort.length != 2) {
            return false;
        }

        String ip = ipPort[0];
        int port = Integer.parseInt(ipPort[1]);
        boolean ephemeral = Boolean.parseBoolean(parts[1]);

        // 只接受临时客户端
        if (!ephemeral) {
            return false;
        }

        Client client = new IpPortBasedClient(clientId, true);
        return clientConnected(client);
    }

    @Override
    public boolean clientConnected(Client client) {
        if (!client.isEphemeral()) {
            return false;
        }

        clients.computeIfAbsent(client.getClientId(), key -> {
            System.out.println("[ClientManager] Connected: " + key);
            return client;
        });
        return true;
    }

    @Override
    public boolean clientDisconnected(String clientId) {
        Client client = clients.remove(clientId);
        if (client != null) {
            // 清理资源
            client.release();
            System.out.println("[ClientManager] Disconnected: " + clientId);
            return true;
        }
        return false;
    }

    @Override
    public Client getClient(String clientId) {
        return clients.get(clientId);
    }

    @Override
    public boolean contains(String clientId) {
        return clients.containsKey(clientId);
    }

    @Override
    public Collection<String> allClientId() {
        return clients.keySet();
    }

    @Override
    public int currentClientCount() {
        return clients.size();
    }
}
