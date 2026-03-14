package com.mynacos.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * Nacos 客户端 - 阶段1：单机 MVP
 * 
 * 功能：
 * - 服务注册
 * - 服务发现
 * - 心跳上报
 */
public class NacosClient {
    
    private final String serverAddr;    // 服务端地址
    private final String namespaceId;   // 命名空间
    private final String groupName;     // 分组名
    
    private final ScheduledExecutorService heartbeatExecutor;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> heartbeatTasks;
    
    public NacosClient(String serverAddr) {
        this(serverAddr, "public", "DEFAULT_GROUP");
    }
    
    public NacosClient(String serverAddr, String namespaceId, String groupName) {
        this.serverAddr = serverAddr;
        this.namespaceId = namespaceId;
        this.groupName = groupName;
        
        this.heartbeatExecutor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "Nacos-Heartbeat");
            t.setDaemon(true);
            return t;
        });
        this.heartbeatTasks = new ConcurrentHashMap<>();
    }
    
    /**
     * 注册实例
     */
    public boolean registerInstance(String serviceName, String ip, int port) {
        try {
            String url = String.format("http://%s/nacos/v1/ns/instance?" +
                    "namespaceId=%s&groupName=%s&serviceName=%s&ip=%s&port=%d",
                serverAddr,
                URLEncoder.encode(namespaceId, StandardCharsets.UTF_8),
                URLEncoder.encode(groupName, StandardCharsets.UTF_8),
                URLEncoder.encode(serviceName, StandardCharsets.UTF_8),
                URLEncoder.encode(ip, StandardCharsets.UTF_8),
                port);
            
            String response = sendRequest(url, "POST");
            boolean success = "ok".equals(response);
            
            if (success) {
                // 启动心跳任务
                startHeartbeat(serviceName, ip, port);
                System.out.println("Registered and started heartbeat: " + serviceName);
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("Register failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 查询实例列表
     */
    public String getInstances(String serviceName) {
        try {
            String url = String.format("http://%s/nacos/v1/ns/instance/list?" +
                    "namespaceId=%s&groupName=%s&serviceName=%s",
                serverAddr,
                URLEncoder.encode(namespaceId, StandardCharsets.UTF_8),
                URLEncoder.encode(groupName, StandardCharsets.UTF_8),
                URLEncoder.encode(serviceName, StandardCharsets.UTF_8));
            
            return sendRequest(url, "GET");
        } catch (Exception e) {
            System.err.println("Query failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 启动心跳任务
     */
    private void startHeartbeat(String serviceName, String ip, int port) {
        String key = serviceName + "#" + ip + "#" + port;
        
        ScheduledFuture<?> future = heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                sendHeartbeat(serviceName, ip, port);
            } catch (Exception e) {
                System.err.println("Heartbeat failed: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);  // 每5秒心跳一次
        
        heartbeatTasks.put(key, future);
    }
    
    /**
     * 发送心跳
     */
    private void sendHeartbeat(String serviceName, String ip, int port) throws Exception {
        String url = String.format("http://%s/nacos/v1/ns/instance/beat?" +
                "namespaceId=%s&groupName=%s&serviceName=%s&ip=%s&port=%d",
            serverAddr,
            URLEncoder.encode(namespaceId, StandardCharsets.UTF_8),
            URLEncoder.encode(groupName, StandardCharsets.UTF_8),
            URLEncoder.encode(serviceName, StandardCharsets.UTF_8),
            URLEncoder.encode(ip, StandardCharsets.UTF_8),
            port);
        
        sendRequest(url, "PUT");
    }
    
    /**
     * 发送 HTTP 请求
     */
    private String sendRequest(String urlStr, String method) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("HTTP error: " + responseCode);
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    /**
     * 关闭客户端
     */
    public void shutdown() {
        // 取消所有心跳任务
        for (ScheduledFuture<?> future : heartbeatTasks.values()) {
            future.cancel(false);
        }
        heartbeatExecutor.shutdown();
    }
}
