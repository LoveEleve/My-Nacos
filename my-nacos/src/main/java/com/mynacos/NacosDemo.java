/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos;

import com.mynacos.naming.core.v2.ServiceManager;
import com.mynacos.naming.core.v2.client.Client;
import com.mynacos.naming.core.v2.client.impl.IpPortBasedClient;
import com.mynacos.naming.core.v2.client.manager.ClientManager;
import com.mynacos.naming.core.v2.client.manager.impl.EphemeralIpPortClientManager;
import com.mynacos.naming.core.v2.pojo.InstancePublishInfo;
import com.mynacos.naming.core.v2.pojo.Service;

/**
 * Nacos v2 核心功能演示
 *
 * 验证:
 * 1. Service 单例模式
 * 2. ServiceManager 管理
 * 3. Client 发布实例
 */
public class NacosDemo {

    public static void main(String[] args) {
        System.out.println("=== My Nacos v2 Core Demo ===\n");

        // 1. 获取 ServiceManager 单例
        ServiceManager serviceManager = ServiceManager.getInstance();
        System.out.println("1. ServiceManager 单例获取成功");

        // 2. 创建或获取 Service 单例
        Service orderService = Service.newService("public", "DEFAULT_GROUP", "order-service");
        Service orderService2 = Service.newService("public", "DEFAULT_GROUP", "order-service");

        // 验证是同一个对象（通过 equals）
        System.out.println("\n2. Service 单例验证:");
        System.out.println("   orderService == orderService2 (equals): " + orderService.equals(orderService2));
        System.out.println("   orderService == orderService2 (hashCode): " + (orderService.hashCode() == orderService2.hashCode()));

        // 通过 ServiceManager 获取单例
        Service singletonOrderService = serviceManager.getSingleton(orderService);
        Service singletonOrderService2 = serviceManager.getSingleton(orderService2);
        System.out.println("   从 ServiceManager 获取的是同一对象: " + (singletonOrderService == singletonOrderService2));

        // 3. 创建另一个服务
        Service userService = Service.newService("public", "DEFAULT_GROUP", "user-service");
        serviceManager.getSingleton(userService);
        System.out.println("\n   ServiceManager 当前服务数: " + serviceManager.size());

        // 4. 创建 ClientManager
        ClientManager clientManager = new EphemeralIpPortClientManager();
        System.out.println("\n3. ClientManager 创建成功");

        // 5. 客户端连接
        String clientId = "192.168.1.100:8080#true";
        clientManager.clientConnected(clientId);
        System.out.println("\n4. 客户端连接成功: " + clientId);
        System.out.println("   当前客户端数: " + clientManager.currentClientCount());

        // 6. 获取 Client 并注册实例
        Client client = clientManager.getClient(clientId);
        if (client != null) {
            InstancePublishInfo instance = new InstancePublishInfo("192.168.1.100", 8080);
            instance.setWeight(1.0);
            instance.setHealthy(true);
            instance.getMetadata().put("version", "v1.0");

            client.addServiceInstance(singletonOrderService, instance);
            System.out.println("\n5. 实例注册成功");
            System.out.println("   服务: " + singletonOrderService.getName());
            System.out.println("   实例: " + instance.toInetAddr());

            // 验证可以获取
            InstancePublishInfo retrieved = client.getInstancePublishInfo(singletonOrderService);
            System.out.println("   验证获取: " + retrieved);
        }

        // 7. 创建另一个客户端（模拟另一个服务实例）
        String clientId2 = "192.168.1.101:8080#true";
        clientManager.clientConnected(clientId2);
        Client client2 = clientManager.getClient(clientId2);

        if (client2 != null) {
            InstancePublishInfo instance2 = new InstancePublishInfo("192.168.1.101", 8080);
            client2.addServiceInstance(singletonOrderService, instance2);
            System.out.println("\n6. 第二个实例注册成功");
        }

        // 8. 查看 Service 版本号
        System.out.println("\n7. Service 版本信息:");
        System.out.println("   order-service revision: " + singletonOrderService.getRevision());
        System.out.println("   order-service lastUpdated: " + singletonOrderService.getLastUpdatedTime());

        // 9. 总结
        System.out.println("\n=== Summary ===");
        System.out.println("Services: " + serviceManager.size());
        System.out.println("Clients: " + clientManager.currentClientCount());
        System.out.println("\nDemo completed!");
    }
}
