/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.remote;

import com.mynacos.naming.core.v2.ServiceInfoHolder;
import com.mynacos.naming.core.v2.pojo.InstancePublishInfo;
import com.mynacos.naming.core.v2.pojo.ServiceInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * NacosClient - 客户端实现
 *
 * 功能：
 * 1. 服务发现（优先本地缓存）
 * 2. 订阅服务（接收服务端推送）
 * 3. 本地缓存（故障转移）
 */
public class NacosClient {

    private final String serverAddr;
    private final ServiceInfoHolder serviceInfoHolder;
    private volatile boolean healthy = true;

    public NacosClient(String serverAddr) {
        this.serverAddr = serverAddr;
        this.serviceInfoHolder = new ServiceInfoHolder("./nacos/cache");

        // 添加缓存更新监听器
        this.serviceInfoHolder.addListener(new ServiceInfoHolder.ServiceInfoUpdateListener() {
            @Override
            public void onUpdate(ServiceInfo serviceInfo) {
                System.out.println("[NacosClient] Cache updated: " + serviceInfo.getName()
                    + " with " + (serviceInfo.getHosts() != null ? serviceInfo.getHosts().size() : 0) + " hosts");
            }
        });
    }

    /**
     * 获取服务实例列表
     *
     * 策略：
     * 1. 先查本地缓存
     * 2. 缓存不存在或已过期，从服务端获取
     * 3. 服务端不可用，进入故障转移模式，使用过期缓存
     */
    public List<InstancePublishInfo> getInstances(String serviceName) throws Exception {
        return getInstances(serviceName, "DEFAULT_GROUP", "");
    }

    /**
     * 获取服务实例列表（带分组和集群）
     */
    public List<InstancePublishInfo> getInstances(String serviceName, String groupName, String clusters) throws Exception {
        // 1. 先查本地缓存
        ServiceInfo cached = serviceInfoHolder.getServiceInfo(serviceName, groupName, clusters);

        if (cached != null && !cached.isExpired()) {
            System.out.println("[NacosClient] Hit local cache: " + serviceName);
            return cached.getHealthyHosts();
        }

        // 2. 缓存过期或不存在，从服务端获取
        try {
            ServiceInfo info = queryFromServer(serviceName, groupName, clusters);

            if (info != null) {
                // 更新本地缓存
                serviceInfoHolder.processServiceInfo(info);

                // 如果之前是故障转移模式，现在恢复了
                if (serviceInfoHolder.isFailoverMode()) {
                    serviceInfoHolder.exitFailoverMode();
                    healthy = true;
                }

                return info.getHealthyHosts();
            }
        } catch (Exception e) {
            System.err.println("[NacosClient] Query server failed: " + e.getMessage());

            // 3. 服务端不可用，进入故障转移模式
            if (!serviceInfoHolder.isFailoverMode()) {
                serviceInfoHolder.enterFailoverMode();
                healthy = false;
            }
        }

        // 4. 故障转移模式下，使用过期的本地缓存
        if (cached != null) {
            System.out.println("[NacosClient] Using failover cache: " + serviceName);
            return cached.getHealthyHosts();
        }

        return null;
    }

    /**
     * 订阅服务
     */
    public boolean subscribe(String serviceName) throws Exception {
        return subscribe(serviceName, "DEFAULT_GROUP", "");
    }

    /**
     * 订阅服务（带分组和集群）
     */
    public boolean subscribe(String serviceName, String groupName, String clusters) throws Exception {
        try {
            String url = "http://" + serverAddr + "/nacos/v1/ns/instance/subscribe?" +
                    "serviceName=" + encode(serviceName) +
                    "&groupName=" + encode(groupName) +
                    "&clusters=" + encode(clusters) +
                    "&clientId=" + encode(getClientId());

            String response = sendRequest(url, "POST");
            return response.contains("\"code\":200");
        } catch (Exception e) {
            System.err.println("[NacosClient] Subscribe failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从服务端查询服务信息
     */
    private ServiceInfo queryFromServer(String serviceName, String groupName, String clusters) throws Exception {
        String url = "http://" + serverAddr + "/nacos/v1/ns/instance/list?" +
                "serviceName=" + encode(serviceName) +
                "&groupName=" + encode(groupName) +
                "&clusters=" + encode(clusters);

        String response = sendRequest(url, "GET");

        // 解析响应（简化实现）
        return parseResponse(response, serviceName, groupName, clusters);
    }

    /**
     * 解析服务端响应
     */
    private ServiceInfo parseResponse(String response, String name, String groupName, String clusters) {
        // 简化实现：直接创建 ServiceInfo
        // 实际应该解析 JSON
        ServiceInfo info = new ServiceInfo();
        info.setName(name);
        info.setGroupName(groupName);
        info.setClusters(clusters);
        info.refreshRefTime();
        return info;
    }

    /**
     * 发送 HTTP 请求
     */
    private String sendRequest(String urlStr, String method) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        int code = conn.getResponseCode();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /**
     * URL 编码
     */
    private String encode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 获取客户端 ID
     */
    private String getClientId() {
        // 简化实现，实际应该基于 IP:Port
        return "client-" + System.currentTimeMillis();
    }

    /**
     * 获取本地缓存管理器
     */
    public ServiceInfoHolder getServiceInfoHolder() {
        return serviceInfoHolder;
    }

    /**
     * 是否健康（未处于故障转移模式）
     */
    public boolean isHealthy() {
        return healthy;
    }
}
