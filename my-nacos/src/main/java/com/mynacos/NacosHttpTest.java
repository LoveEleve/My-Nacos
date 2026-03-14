/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * HTTP 接口测试
 *
 * 测试场景：
 * 1. 注册实例
 * 2. 查询实例列表
 * 3. 发送心跳
 * 4. 注销实例
 */
public class NacosHttpTest {

    private static final String SERVER_URL = "http://localhost:8848";

    public static void main(String[] args) throws Exception {
        System.out.println("=== Nacos HTTP API Test ===\n");

        // 等待服务器启动
        Thread.sleep(1000);

        // 1. 注册实例
        System.out.println("1. Register Instance:");
        String registerResult = registerInstance("order-service", "192.168.1.100", 8080);
        System.out.println("   Result: " + registerResult);

        // 2. 注册另一个实例
        System.out.println("\n2. Register Another Instance:");
        String registerResult2 = registerInstance("order-service", "192.168.1.101", 8080);
        System.out.println("   Result: " + registerResult2);

        // 3. 查询实例列表
        System.out.println("\n3. Query Instances:");
        Thread.sleep(500); // 等待注册完成
        String listResult = listInstances("order-service");
        System.out.println("   Result: " + formatJson(listResult));

        // 4. 发送心跳
        System.out.println("\n4. Send Heartbeat:");
        String beatResult = sendHeartbeat("order-service", "192.168.1.100", 8080);
        System.out.println("   Result: " + beatResult);

        // 5. 注销实例
        System.out.println("\n5. Deregister Instance:");
        String deregisterResult = deregisterInstance("order-service", "192.168.1.100", 8080);
        System.out.println("   Result: " + deregisterResult);

        // 6. 再次查询
        System.out.println("\n6. Query After Deregister:");
        Thread.sleep(500);
        String listResult2 = listInstances("order-service");
        System.out.println("   Result: " + formatJson(listResult2));

        System.out.println("\n=== Test Completed ===");
    }

    /**
     * 注册实例
     */
    private static String registerInstance(String serviceName, String ip, int port) throws Exception {
        String url = SERVER_URL + "/nacos/v1/ns/instance?" +
                "serviceName=" + encode(serviceName) +
                "&ip=" + encode(ip) +
                "&port=" + port +
                "&weight=1.0" +
                "&healthy=true";

        return sendRequest(url, "POST");
    }

    /**
     * 查询实例列表
     */
    private static String listInstances(String serviceName) throws Exception {
        String url = SERVER_URL + "/nacos/v1/ns/instance/list?" +
                "serviceName=" + encode(serviceName);

        return sendRequest(url, "GET");
    }

    /**
     * 发送心跳
     */
    private static String sendHeartbeat(String serviceName, String ip, int port) throws Exception {
        String url = SERVER_URL + "/nacos/v1/ns/instance/beat?" +
                "serviceName=" + encode(serviceName) +
                "&ip=" + encode(ip) +
                "&port=" + port;

        return sendRequest(url, "PUT");
    }

    /**
     * 注销实例
     */
    private static String deregisterInstance(String serviceName, String ip, int port) throws Exception {
        String url = SERVER_URL + "/nacos/v1/ns/instance?" +
                "serviceName=" + encode(serviceName) +
                "&ip=" + encode(ip) +
                "&port=" + port;

        return sendRequest(url, "DELETE");
    }

    /**
     * 发送 HTTP 请求
     */
    private static String sendRequest(String urlStr, String method) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            return "Error: " + responseCode;
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
     * URL 编码
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 简单格式化 JSON（仅用于展示）
     */
    private static String formatJson(String json) {
        if (json.length() > 200) {
            return json.substring(0, 200) + "...";
        }
        return json;
    }
}
