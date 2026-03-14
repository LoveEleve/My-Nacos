/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos;

import com.mynacos.naming.remote.InstanceController;

/**
 * 推送机制测试
 */
public class NacosPushTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Nacos Push Mechanism Test ===\n");

        InstanceController controller = new InstanceController();

        // 1. 模拟客户端1订阅服务
        System.out.println("1. Client1 subscribe to order-service:");
        String sub1 = controller.subscribe("public", "DEFAULT_GROUP", "order-service", "client-192.168.1.200");
        System.out.println("   " + sub1);

        // 2. 模拟客户端2订阅服务
        System.out.println("\n2. Client2 subscribe to order-service:");
        String sub2 = controller.subscribe("public", "DEFAULT_GROUP", "order-service", "client-192.168.1.201");
        System.out.println("   " + sub2);

        Thread.sleep(1000);

        // 3. 注册实例（应该触发推送给两个客户端）
        System.out.println("\n3. Register instance (should trigger push to all subscribers):");
        String reg1 = controller.registerInstance(
            "public", "DEFAULT_GROUP", "order-service",
            "192.168.1.100", 8080, 1.0, true, null);
        System.out.println("   " + reg1);

        Thread.sleep(1000);

        // 4. 注册第二个实例
        System.out.println("\n4. Register another instance:");
        String reg2 = controller.registerInstance(
            "public", "DEFAULT_GROUP", "order-service",
            "192.168.1.101", 8081, 1.0, true, null);
        System.out.println("   " + reg2);

        // 等待推送完成
        Thread.sleep(2000);

        // 5. 查询实例
        System.out.println("\n5. Query instances:");
        String list = controller.listInstances("public", "DEFAULT_GROUP", "order-service");
        System.out.println("   " + list.substring(0, Math.min(200, list.length())) + "...");

        // 6. 注销实例（应该触发推送）
        System.out.println("\n6. Deregister instance (should trigger push):");
        String dereg = controller.deregisterInstance(
            "public", "DEFAULT_GROUP", "order-service",
            "192.168.1.100", 8080);
        System.out.println("   " + dereg);

        Thread.sleep(2000);

        System.out.println("\n=== Push Test Completed ===");
    }
}
