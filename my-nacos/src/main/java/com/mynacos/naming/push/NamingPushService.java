/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.push;

import com.mynacos.naming.core.v2.index.ClientServiceIndexesManager;
import com.mynacos.naming.core.v2.index.ServiceStorage;
import com.mynacos.naming.core.v2.pojo.InstancePublishInfo;
import com.mynacos.naming.core.v2.pojo.Service;

import java.util.List;
import java.util.Set;

/**
 * NamingPushService - 命名服务推送实现
 *
 * 职责：
 * 1. 获取服务的实例列表
 * 2. 获取订阅该服务的客户端
 * 3. 向客户端推送数据
 *
 * 对照：com.alibaba.nacos.naming.push.v2.NamingPushService
 */
public class NamingPushService implements PushTaskProcessor {

    private final ClientServiceIndexesManager indexesManager;
    private final ServiceStorage serviceStorage;

    public NamingPushService() {
        this.indexesManager = ClientServiceIndexesManager.getInstance();
        this.serviceStorage = ServiceStorage.getInstance();
    }

    @Override
    public void process(PushDelayTask task) {
        Service service = task.getService();

        // 1. 获取服务的实例列表
        List<InstancePublishInfo> instances = serviceStorage.getPushData(service);

        // 2. 生成推送数据
        String pushData = serviceStorage.getServiceInfo(service, instances);

        // 3. 获取目标客户端
        Set<String> targetClients;
        if (task.isPushToAll()) {
            // 推送给所有订阅者
            targetClients = indexesManager.getSubscribers(service);
        } else {
            // 推送给指定客户端
            targetClients = task.getTargetClients();
        }

        // 4. 执行推送
        if (targetClients != null && !targetClients.isEmpty()) {
            for (String clientId : targetClients) {
                pushToClient(clientId, service, pushData);
            }
        }

        System.out.println("[Push] Service " + service.getName()
            + " pushed to " + (targetClients != null ? targetClients.size() : 0) + " clients");
    }

    /**
     * 向指定客户端推送数据
     *
     * @param clientId  客户端ID
     * @param service   服务
     * @param pushData  推送数据
     */
    private void pushToClient(String clientId, Service service, String pushData) {
        // TODO: 实际推送（gRPC/HTTP）
        // 目前只打印日志
        System.out.println("[Push] To " + clientId + ": " + pushData.substring(0, Math.min(80, pushData.length())) + "...");
    }
}
