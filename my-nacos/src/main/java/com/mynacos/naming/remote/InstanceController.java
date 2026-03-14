/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.remote;

import com.mynacos.naming.core.v2.ServiceManager;
import com.mynacos.naming.core.v2.client.Client;
import com.mynacos.naming.core.v2.client.impl.IpPortBasedClient;
import com.mynacos.naming.core.v2.client.manager.ClientManager;
import com.mynacos.naming.core.v2.client.manager.impl.EphemeralIpPortClientManager;
import com.mynacos.naming.core.v2.index.ClientServiceIndexesManager;
import com.mynacos.naming.core.v2.pojo.InstancePublishInfo;
import com.mynacos.naming.core.v2.pojo.Service;
import com.mynacos.naming.push.NamingPushService;
import com.mynacos.naming.push.PushDelayTask;
import com.mynacos.naming.push.PushDelayTaskExecuteEngine;

import java.util.Collection;
import java.util.Optional;

/**
 * InstanceController - 实例管理控制器
 *
 * 新增：订阅功能和推送触发
 */
public class InstanceController {

    private final ServiceManager serviceManager;
    private final ClientManager clientManager;
    private final ClientServiceIndexesManager indexesManager;
    private final PushDelayTaskExecuteEngine pushEngine;

    public InstanceController() {
        this.serviceManager = ServiceManager.getInstance();
        this.clientManager = new EphemeralIpPortClientManager();
        this.indexesManager = ClientServiceIndexesManager.getInstance();

        // 初始化推送引擎
        NamingPushService pushService = new NamingPushService();
        this.pushEngine = new PushDelayTaskExecuteEngine(pushService);
        this.pushEngine.start();
    }

    /**
     * 注册实例
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

            // 5. 触发推送（延迟500ms）
            triggerPush(singletonService);

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
            Optional<Service> serviceOpt = serviceManager.getSingletonIfExist(
                namespaceId, groupName, serviceName);

            if (!serviceOpt.isPresent()) {
                return "{\"code\":200,\"message\":\"service not found\"}";
            }

            String clientId = ip + ":" + port + "#true";
            Client client = clientManager.getClient(clientId);

            if (client == null) {
                return "{\"code\":200,\"message\":\"client not found\"}";
            }

            Service service = serviceOpt.get();
            client.removeServiceInstance(service);

            // 清理订阅
            indexesManager.removeSubscriber(service, clientId);

            if (client.getAllPublishedService().isEmpty()) {
                clientManager.clientDisconnected(clientId);
            }

            // 触发推送
            triggerPush(service);

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
            Optional<Service> serviceOpt = serviceManager.getSingletonIfExist(
                namespaceId, groupName, serviceName);

            if (!serviceOpt.isPresent()) {
                return buildEmptyResponse(serviceName);
            }

            Service service = serviceOpt.get();

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
                return "{\"code\":20404,\"message\":\"client not found\"}";
            }

            client.setLastUpdatedTime();

            Optional<Service> serviceOpt = serviceManager.getSingletonIfExist(
                namespaceId, groupName, serviceName);

            if (serviceOpt.isPresent()) {
                Service service = serviceOpt.get();
                InstancePublishInfo instance = client.getInstancePublishInfo(service);
                if (instance != null && !instance.isHealthy()) {
                    instance.setHealthy(true);
                }
            }

            return "{\"code\":200,\"message\":\"ok\"}";
        } catch (Exception e) {
            return "{\"code\":500,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 订阅服务
     */
    public String subscribe(String namespaceId, String groupName,
                           String serviceName, String clientId) {
        try {
            Service service = Service.newService(namespaceId, groupName, serviceName);
            Service singletonService = serviceManager.getSingleton(service);

            // 添加订阅关系
            indexesManager.addSubscriber(singletonService, clientId);

            // 立即推送一次（延迟0ms，专门给这个客户端）
            PushDelayTask task = new PushDelayTask(singletonService, clientId);
            pushEngine.addTask(singletonService, task);

            return "{\"code\":200,\"message\":\"ok\"}";
        } catch (Exception e) {
            return "{\"code\":500,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 取消订阅
     */
    public String unsubscribe(String namespaceId, String groupName,
                             String serviceName, String clientId) {
        try {
            Optional<Service> serviceOpt = serviceManager.getSingletonIfExist(
                namespaceId, groupName, serviceName);

            if (serviceOpt.isPresent()) {
                indexesManager.removeSubscriber(serviceOpt.get(), clientId);
            }

            return "{\"code\":200,\"message\":\"ok\"}";
        } catch (Exception e) {
            return "{\"code\":500,\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 触发推送
     */
    private void triggerPush(Service service) {
        PushDelayTask task = new PushDelayTask(service);
        pushEngine.addTask(service, task);
    }

    private String buildEmptyResponse(String serviceName) {
        return "{\"name\":\"" + serviceName + "\",\"groupName\":\"DEFAULT_GROUP\",\"clusters\":\"\",\"cacheMillis\":10000,\"hosts\":[]}";
    }

    public ClientManager getClientManager() {
        return clientManager;
    }
}
