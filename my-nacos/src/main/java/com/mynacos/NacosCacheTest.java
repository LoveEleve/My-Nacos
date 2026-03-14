/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos;

import com.mynacos.naming.core.v2.ServiceInfoHolder;
import com.mynacos.naming.core.v2.pojo.InstancePublishInfo;
import com.mynacos.naming.core.v2.pojo.ServiceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地缓存和故障转移测试
 */
public class NacosCacheTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Nacos Cache & Failover Test ===\n");

        // 1. 创建 ServiceInfoHolder
        System.out.println("1. Create ServiceInfoHolder:");
        ServiceInfoHolder holder = new ServiceInfoHolder("./test-cache");
        System.out.println("   Cache dir: ./test-cache");

        // 2. 模拟服务端推送的数据
        System.out.println("\n2. Simulate server push:");
        ServiceInfo serviceInfo = createServiceInfo("order-service", "DEFAULT_GROUP", "");
        serviceInfo.setHosts(createHosts());
        serviceInfo.refreshRefTime();

        // 3. 更新本地缓存
        System.out.println("\n3. Update local cache:");
        boolean updated = holder.processServiceInfo(serviceInfo);
        System.out.println("   Updated: " + updated);

        // 4. 从缓存读取
        System.out.println("\n4. Read from cache:");
        ServiceInfo cached = holder.getServiceInfo("order-service", "DEFAULT_GROUP", "");
        System.out.println("   Cached: " + cached);
        System.out.println("   Hosts count: " + (cached != null && cached.getHosts() != null ? cached.getHosts().size() : 0));

        // 5. 模拟服务端故障，进入故障转移模式
        System.out.println("\n5. Enter failover mode:");
        holder.enterFailoverMode();
        System.out.println("   Failover mode: " + holder.isFailoverMode());

        // 6. 故障转移模式下仍能读取缓存（即使过期）
        System.out.println("\n6. Read in failover mode:");
        ServiceInfo failoverCached = holder.getServiceInfoSafe("order-service", "DEFAULT_GROUP", "");
        System.out.println("   Failover cached: " + failoverCached);

        // 7. 退出故障转移模式
        System.out.println("\n7. Exit failover mode:");
        holder.exitFailoverMode();
        System.out.println("   Failover mode: " + holder.isFailoverMode());

        // 8. 测试过期判断
        System.out.println("\n8. Check expired:");
        System.out.println("   Is expired: " + cached.isExpired());

        System.out.println("\n=== Cache Test Completed ===");
    }

    /**
     * 创建 ServiceInfo
     */
    private static ServiceInfo createServiceInfo(String name, String groupName, String clusters) {
        ServiceInfo info = new ServiceInfo();
        info.setName(name);
        info.setGroupName(groupName);
        info.setClusters(clusters);
        info.setCacheMillis(10000); // 10秒过期
        return info;
    }

    /**
     * 创建测试实例列表
     */
    private static List<InstancePublishInfo> createHosts() {
        List<InstancePublishInfo> hosts = new ArrayList<>();

        InstancePublishInfo i1 = new InstancePublishInfo("192.168.1.100", 8080);
        i1.setWeight(1.0);
        i1.setHealthy(true);
        hosts.add(i1);

        InstancePublishInfo i2 = new InstancePublishInfo("192.168.1.101", 8081);
        i2.setWeight(1.0);
        i2.setHealthy(true);
        hosts.add(i2);

        return hosts;
    }
}
