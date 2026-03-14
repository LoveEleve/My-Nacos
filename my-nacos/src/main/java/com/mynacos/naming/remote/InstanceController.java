/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.remote;

import com.mynacos.naming.core.v2.ServiceManager;
import com.mynacos.naming.core.v2.client.Client;
import com.mynacos.naming.core.v2.client.impl.IpPortBasedClient;
import com.mynacos.naming.core.v2.client.manager.ClientManager;
import com.mynacos.naming.core.v2.client.manager.impl.EphemeralIpPortClientManager;
import com.mynacos.naming.core.v2.pojo.InstancePublishInfo;
import com.mynacos.naming.core.v2.pojo.Service;

import java.util.Collection;
import java.util.Optional;

/**
 * InstanceController - 实例管理控制器
 *
 * 问题：外部如何调用 Nacos 的服务注册/发现功能？
 *
 * 方案：HTTP 接口
 * - POST /nacos/v1/ns/instance - 注册实例
 * - DELETE /nacos/v1/ns/instance - 注销实例
 * - GET /nacos/v1/ns/instance/list - 查询实例列表
 * - PUT /nacos/v1/ns/instance/beat - 心跳
 *
 * 对照：com.alibaba.nacos.naming.controllers.InstanceController
 */
public class InstanceController {

    private final ServiceManager serviceManager;
    private final ClientManager clientManager;

    public InstanceController() {
        this.serviceManager = ServiceManager.getInstance();
        this.clientManager = new EphemeralIpPortClientManager();
    }

    /**
     * 注册实例
     *
     * 流程：
     * 1. 获取或创建 Service 单例
     * 2. 获取或创建 Client
     * 3. Client 发布实例
     */
    public String registerInstance(String namespaceId, String groupName,
                                   String serviceName, String ip, int port,
                                   double weight, boolean healthy,
                                   java.util.Map<String, String> metadata) {
        try {
            // 1. 获取 Service 单例
            Service service = Service.newService(namespaceId, groupName, serviceName);
            Service singletonService = serviceManager.getSingleton(service);

            // 2. 获取或创建 Client
            String clientId = ip + ":" + port + "#true";
            Client client = clientManager.getClient(clientId);
            if (client == null) {
                clientManager.clientConnected(clientId);
                client = clientManager.getClient(clientId);
            }

            if (client == null) {
                return "{\"code\":500,\"message\":\"Failed to create client\"}";
            }

            // 3. 创建实例信息
            InstancePublishInfo instanceInfo = new InstancePublishInfo(ip, port);
            instanceInfo.setWeight(weight);
            instanceInfo.setHealthy(healthy);
            if (metadata != null) {
                instanceInfo.getMetadata().putAll(metadata);
            }

            // 4. 发布实例
            client.addServiceInstance(singletonService, instanceInfo);

            return "{\"code\":200,\"message\":\"ok\"}";
        } catch (Exception e) {
            return "{\"code\":500,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 注销实例
     */
    public String deregisterInstance(String namespaceId, String groupName,
                                     String serviceName, String ip, int port) {
        try {
            // 1. 查找 Service
            Optional<Service> serviceOpt = serviceManager.getSingletonIfExist(
                namespaceId, groupName, serviceName);

            if (!serviceOpt.isPresent()) {
                return "{\"code\":200,\"message\":\"service not found\"}";
            }

            // 2. 查找 Client
            String clientId = ip + ":" + port + "#true";
            Client client = clientManager.getClient(clientId);

            if (client == null) {
                return "{\"code\":200,\"message\":\"client not found\"}";
            }

            // 3. 移除实例
            client.removeServiceInstance(serviceOpt.get());

            // 4. 如果 Client 没有实例了，断开连接
            if (client.getAllPublishedService().isEmpty()) {
                clientManager.clientDisconnected(clientId);
            }

            return "{\"code\":200,\"message\":\"ok\"}";
        } catch (Exception e) {
            return "{\"code\":500,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 查询实例列表
     */
    public String listInstances(String namespaceId, String groupName, String serviceName) {
        try {
            // 1. 查找 Service
            Optional<Service> serviceOpt = serviceManager.getSingletonIfExist(
                namespaceId, groupName, serviceName);

            if (!serviceOpt.isPresent()) {
                return buildEmptyResponse(serviceName);
            }

            Service service = serviceOpt.get();

            // 2. 收集所有 Client 发布的该服务实例
            StringBuilder hosts = new StringBuilder();
            hosts.append("[");

            boolean first = true;
            for (String clientId : clientManager.allClientId()) {
                Client client = clientManager.getClient(clientId);
                if (client != null && client.containsService(service)) {
                    InstancePublishInfo instance = client.getInstancePublishInfo(service);
                    if (instance != null) {
                        if (!first) hosts.append(",");
                        first = false;

                        hosts.append("{");
                        hosts.append("\"ip\":\"").append(instance.getIp()).append("\",");
                        hosts.append("\"port\":").append(instance.getPort()).append(",");
                        hosts.append("\"weight\":").append(instance.getWeight()).append(",");
                        hosts.append("\"healthy\":").append(instance.isHealthy()).append(",");
                        hosts.append("\"enabled\":").append(instance.isEnabled());
                        hosts.append("}");
                    }
                }
            }

            hosts.append("]");

            // 3. 构建响应
            StringBuilder response = new StringBuilder();
            response.append("{");
            response.append("\"name\":\"").append(serviceName).append("\",");
            response.append("\"groupName\":\"").append(groupName).append("\",");
            response.append("\"clusters\":\"\",");
            response.append("\"cacheMillis\":10000,");
            response.append("\"hosts\":").append(hosts);
            response.append("}");

            return response.toString();
        } catch (Exception e) {
            return "{\"code\":500,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 心跳
     */
    public String beat(String namespaceId, String groupName,
                       String serviceName, String ip, int port) {
        try {
            String clientId = ip + ":" + port + "#true";
            Client client = clientManager.getClient(clientId);

            if (client == null) {
                // Client 不存在，需要重新注册
                return "{\"code\":20404,\"message\":\"client not found\"}";
            }

            // 刷新客户端更新时间
            client.setLastUpdatedTime();

            // 查找 Service 并更新
            Optional<Service> serviceOpt = serviceManager.getSingletonIfExist(
                namespaceId, groupName, serviceName);

            if (serviceOpt.isPresent()) {
                Service service = serviceOpt.get();
                InstancePublishInfo instance = client.getInstancePublishInfo(service);
                if (instance != null && !instance.isHealthy()) {
                    // 如果之前不健康，恢复为健康
                    instance.setHealthy(true);
                }
            }

            return "{\"code\":200,\"message\":\"ok\"}";
        } catch (Exception e) {
            return "{\"code\":500,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 构建空响应
     */
    private String buildEmptyResponse(String serviceName) {
        return "{\"name\":\"" + serviceName + "\",\"groupName\":\"DEFAULT_GROUP\",\"clusters\":\"\",\"cacheMillis\":10000,\"hosts\":[]}";
    }

    public ClientManager getClientManager() {
        return clientManager;
    }
}
