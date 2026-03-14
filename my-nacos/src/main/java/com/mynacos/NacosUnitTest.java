/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos;

import com.mynacos.naming.remote.InstanceController;

/**
 * 单元测试 - 仅测试 Controller 逻辑
 */
public class NacosUnitTest {

    public static void main(String[] args) {
        System.out.println("=== Nacos Unit Test ===\n");

        InstanceController controller = new InstanceController();

        // 1. 注册实例
        System.out.println("1. Register instance:");
        String r1 = controller.registerInstance(
            "public", "DEFAULT_GROUP", "order-service",
            "192.168.1.100", 8080, 1.0, true, null);
        System.out.println("   Result: " + r1);

        // 2. 再注册一个
        System.out.println("\n2. Register another instance:");
        String r2 = controller.registerInstance(
            "public", "DEFAULT_GROUP", "order-service",
            "192.168.1.101", 8081, 1.0, true, null);
        System.out.println("   Result: " + r2);

        // 3. 查询
        System.out.println("\n3. Query instances:");
        String r3 = controller.listInstances(
            "public", "DEFAULT_GROUP", "order-service");
        System.out.println("   Result: " + r3);

        // 4. 验证包含两个实例
        boolean has100 = r3.contains("192.168.1.100");
        boolean has101 = r3.contains("192.168.1.101");
        System.out.println("   Contains 192.168.1.100: " + has100);
        System.out.println("   Contains 192.168.1.101: " + has101);

        // 5. 注销
        System.out.println("\n4. Deregister first instance:");
        String r4 = controller.deregisterInstance(
            "public", "DEFAULT_GROUP", "order-service",
            "192.168.1.100", 8080);
        System.out.println("   Result: " + r4);

        // 6. 再次查询
        System.out.println("\n5. Query after deregister:");
        String r5 = controller.listInstances(
            "public", "DEFAULT_GROUP", "order-service");
        System.out.println("   Result: " + r5);

        boolean stillHas100 = r5.contains("192.168.1.100");
        boolean stillHas101 = r5.contains("192.168.1.101");
        System.out.println("   Contains 192.168.1.100: " + stillHas100);
        System.out.println("   Contains 192.168.1.101: " + stillHas101);

        // 验证结果
        System.out.println("\n=== Test Result ===");
        if (has100 && has101 && !stillHas100 && stillHas101) {
            System.out.println("✅ All tests PASSED!");
        } else {
            System.out.println("❌ Some tests FAILED!");
        }
    }
}
