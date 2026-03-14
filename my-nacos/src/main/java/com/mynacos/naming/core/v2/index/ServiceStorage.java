/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2.index;

import com.mynacos.naming.core.v2.client.Client;
import com.mynacos.naming.core.v2.client.manager.ClientManager;
import com.mynacos.naming.core.v2.client.manager.impl.EphemeralIpPortClientManager;
import com.mynacos.naming.core.v2.pojo.InstancePublishInfo;
import com.mynacos.naming.core.v2.pojo.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ServiceStorage - 服务存储聚合
 *
 * 职责：
 * - 从所有 Client 中聚合 Service 的实例列表
 * - 生成推送数据（ServiceInfo）
 *
 * 对照：com.alibaba.nacos.naming.core.v2.index.ServiceStorage
 */
public class ServiceStorage {

    private static final ServiceStorage INSTANCE = new ServiceStorage();

    private final ClientManager clientManager;

    private ServiceStorage() {
        this.clientManager = new EphemeralIpPortClientManager();
    }

    public static ServiceStorage getInstance() {
        return INSTANCE;
    }

    /**
     * 获取服务的实例列表（用于推送）
     *
     * @param service 服务
     * @return 实例列表
     */
    public List<InstancePublishInfo> getPushData(Service service) {
        List<InstancePublishInfo> instances = new ArrayList<>();

        // 遍历所有 Client，收集该服务的实例
        for (String clientId : clientManager.allClientId()) {
            Client client = clientManager.getClient(clientId);
            if (client != null && client.containsService(service)) {
                InstancePublishInfo instance = client.getInstancePublishInfo(service);
                if (instance != null) {
                    instances.add(instance);
                }
            }
        }

        return instances;
    }

    /**
     * 生成 ServiceInfo JSON（用于推送）
     */
    public String getServiceInfo(Service service, List<InstancePublishInfo> instances) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"").append(service.getName()).append("\",");
        sb.append("\"groupName\":\"").append(service.getGroup()).append("\",");
        sb.append("\"clusters\":\"\",");
        sb.append("\"cacheMillis\":10000,");
        sb.append("\"hosts\":[");

        for (int i = 0; i < instances.size(); i++) {
            InstancePublishInfo inst = instances.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"ip\":\"").append(inst.getIp()).append("\",");
            sb.append("\"port\":").append(inst.getPort()).append(",");
            sb.append("\"weight\":").append(inst.getWeight()).append(",");
            sb.append("\"healthy\":").append(inst.isHealthy()).append(",");
            sb.append("\"enabled\":").append(inst.isEnabled());
            sb.append("}");
        }

        sb.append("]}");
        return sb.toString();
    }
}
