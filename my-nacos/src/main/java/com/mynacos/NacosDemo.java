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
 * 验证流程：
 * 1. Service 单例模式
 * 2. ServiceManager 管理
 * 3. Client 发布实例
 * 4. ClientManager 管理客户端
 */
public class NacosDemo {

    public static void main(String[] args) {
        System.out.println("=== My Nacos v2 Core Demo ===\n");

        // ========== 1. ServiceManager 单例 ==========
        ServiceManager serviceManager = ServiceManager.getInstance();
        System.out.println("1. ServiceManager singleton: OK\n");

        // ========== 2. Service 单例验证 ==========
        System.out.println("2. Service Singleton Test:");
        
        Service orderService = Service.newService("public", "DEFAULT_GROUP", "order-service");
        Service orderService2 = Service.newService("public", "DEFAULT_GROUP", "order-service");
        
        System.out.println("   orderService.equals(orderService2): " + orderService.equals(orderService2));
        
        Service singleton1 = serviceManager.getSingleton(orderService);
        Service singleton2 = serviceManager.getSingleton(orderService2);
        System.out.println("   Same instance from ServiceManager: " + (singleton1 == singleton2));
        
        // 创建另一个服务
        Service userService = Service.newService("public", "DEFAULT_GROUP", "user-service");
        serviceManager.getSingleton(userService);
        System.out.println("   Total services: " + serviceManager.size() + "\n");

        // ========== 3. Client 管理 ==========
        System.out.println("3. Client Management:");
        
        ClientManager clientManager = new EphemeralIpPortClientManager();
        
        // 客户端连接
        String clientId = "192.168.1.100:8080#true";
        clientManager.clientConnected(clientId);
        System.out.println("   Client connected: " + clientId);
        System.out.println("   Total clients: " + clientManager.currentClientCount());

        // ========== 4. 服务注册 ==========
        System.out.println("\n4. Service Registration:");
        
        Client client = clientManager.getClient(clientId);
        if (client != null) {
            InstancePublishInfo instance = new InstancePublishInfo("192.168.1.100", 8080);
            instance.setWeight(1.0);
            instance.setHealthy(true);
            instance.getMetadata().put("version", "v1.0");

            client.addServiceInstance(singleton1, instance);
            
            // 验证获取
            InstancePublishInfo retrieved = client.getInstancePublishInfo(singleton1);
            System.out.println("   Retrieved: " + retrieved);
        }

        // 第二个实例
        String clientId2 = "192.168.1.101:8080#true";
        clientManager.clientConnected(clientId2);
        Client client2 = clientManager.getClient(clientId2);
        if (client2 != null) {
            InstancePublishInfo instance2 = new InstancePublishInfo("192.168.1.101", 8080);
            client2.addServiceInstance(singleton1, instance2);
        }

        // ========== 5. Service 版本 ==========
        System.out.println("\n5. Service Version:");
        System.out.println("   order-service revision: " + singleton1.getRevision());

        // ========== Summary ==========
        System.out.println("\n=== Summary ===");
        System.out.println("Services: " + serviceManager.size());
        System.out.println("Clients: " + clientManager.currentClientCount());
        System.out.println("\nDemo completed!");
    }
}
