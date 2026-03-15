/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos;

import com.mynacos.naming.consistency.distro.DistroProtocol;
import com.mynacos.naming.core.v2.ServerMemberManager;
import com.mynacos.naming.core.v2.pojo.Member;
import com.mynacos.naming.core.v2.pojo.Service;

/**
 * Distro 协议测试
 */
public class NacosDistroTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Nacos Distro Protocol Test ===\n");

        // 1. 初始化成员管理器（模拟 3 节点集群）
        System.out.println("1. Initialize 3-node cluster:");
        ServerMemberManager memberManager = ServerMemberManager.getInstance();

        // 节点1（当前节点）
        memberManager.initSelf("192.168.1.10", 8848);
        System.out.println("   Self: 192.168.1.10:8848");

        // 节点2
        Member member2 = new Member("192.168.1.11", 8848);
        memberManager.updateMember(member2);
        System.out.println("   Member2: 192.168.1.11:8848");

        // 节点3
        Member member3 = new Member("192.168.1.12", 8848);
        memberManager.updateMember(member3);
        System.out.println("   Member3: 192.168.1.12:8848");

        System.out.println("   Total members: " + memberManager.getMemberCount());

        // 2. 启动 Distro 协议
        System.out.println("\n2. Start Distro protocol:");
        DistroProtocol distroProtocol = new DistroProtocol();
        distroProtocol.start();
        Thread.sleep(500);

        // 3. 测试数据归属
        System.out.println("\n3. Test data responsibility:");
        Service service1 = Service.newService("public", "DEFAULT_GROUP", "order-service");
        Service service2 = Service.newService("public", "DEFAULT_GROUP", "user-service");
        Service service3 = Service.newService("public", "DEFAULT_GROUP", "inventory-service");

        Member responsible1 = distroProtocol.getResponsibleMember(service1);
        Member responsible2 = distroProtocol.getResponsibleMember(service2);
        Member responsible3 = distroProtocol.getResponsibleMember(service3);

        System.out.println("   order-service -> " + (responsible1 != null ? responsible1.getAddress() : "null"));
        System.out.println("   user-service -> " + (responsible2 != null ? responsible2.getAddress() : "null"));
        System.out.println("   inventory-service -> " + (responsible3 != null ? responsible3.getAddress() : "null"));

        // 4. 测试是否本节点负责
        System.out.println("\n4. Test if self is responsible:");
        System.out.println("   Is self responsible for order-service? " + distroProtocol.isResponsible(service1));
        System.out.println("   Is self responsible for user-service? " + distroProtocol.isResponsible(service2));
        System.out.println("   Is self responsible for inventory-service? " + distroProtocol.isResponsible(service3));

        // 5. 模拟数据写入
        System.out.println("\n5. Simulate data write:");
        boolean put1 = distroProtocol.onPut(service1, "data1".getBytes());
        boolean put2 = distroProtocol.onPut(service2, "data2".getBytes());
        boolean put3 = distroProtocol.onPut(service3, "data3".getBytes());

        System.out.println("   Put order-service: " + (put1 ? "local" : "forwarded"));
        System.out.println("   Put user-service: " + (put2 ? "local" : "forwarded"));
        System.out.println("   Put inventory-service: " + (put3 ? "local" : "forwarded"));

        // 6. 模拟节点加入
        System.out.println("\n6. Simulate new member join:");
        Member member4 = new Member("192.168.1.13", 8848);
        memberManager.updateMember(member4);
        distroProtocol.onMemberJoin(member4);

        // 7. 模拟节点离开
        System.out.println("\n7. Simulate member leave:");
        distroProtocol.onMemberLeave(member2);

        Thread.sleep(1000);

        // 8. 停止
        System.out.println("\n8. Stop Distro protocol:");
        distroProtocol.stop();

        System.out.println("\n=== Distro Test Completed ===");
    }
}
