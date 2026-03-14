/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos;

import com.mynacos.naming.remote.NacosHttpServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 集成测试 - 在同一 JVM 中启动服务器和测试
 */
public class NacosIntegrationTest {

    private static final String SERVER_URL = "http://localhost:8849";

    public static void main(String[] args) throws Exception {
        System.out.println("=== Nacos Integration Test ===\n");

        // 1. 启动服务器
        System.out.println("1. Starting server...");
        NacosHttpServer server = new NacosHttpServer(8849);
        server.start();
        Thread.sleep(1000); // 等待服务器启动

        try {
            // 2. 测试注册
            System.out.println("\n2. Test Register:");
            String registerResult = register("order-service", "192.168.1.100", 8080);
            System.out.println("   " + registerResult);

            // 3. 注册第二个实例
            System.out.println("\n3. Register another instance:");
            String registerResult2 = register("order-service", "192.168.1.101", 8081);
            System.out.println("   " + registerResult2);

            // 4. 查询列表
            System.out.println("\n4. Query instance list:");
            String listResult = query("order-service");
            System.out.println("   " + formatJson(listResult));

            // 5. 心跳
            System.out.println("\n5. Send heartbeat:");
            String beatResult = heartbeat("order-service", "192.168.1.100", 8080);
            System.out.println("   " + beatResult);

            // 6. 注销
            System.out.println("\n6. Deregister instance:");
            String deregisterResult = deregister("order-service", "192.168.1.100", 8080);
            System.out.println("   " + deregisterResult);

            // 7. 再次查询
            System.out.println("\n7. Query after deregister:");
            String listResult2 = query("order-service");
            System.out.println("   " + formatJson(listResult2));

            System.out.println("\n=== Test Passed! ===");

        } finally {
            server.stop();
        }
    }

    private static String register(String serviceName, String ip, int port) throws Exception {
        String url = SERVER_URL + "/nacos/v1/ns/instance?" +
                "serviceName=" + encode(serviceName) +
                "&ip=" + encode(ip) +
                "&port=" + port +
                "&weight=1.0" +
                "&healthy=true";
        return sendRequest(url, "POST");
    }

    private static String query(String serviceName) throws Exception {
        String url = SERVER_URL + "/nacos/v1/ns/instance/list?" +
                "serviceName=" + encode(serviceName);
        return sendRequest(url, "GET");
    }

    private static String heartbeat(String serviceName, String ip, int port) throws Exception {
        String url = SERVER_URL + "/nacos/v1/ns/instance/beat?" +
                "serviceName=" + encode(serviceName) +
                "&ip=" + encode(ip) +
                "&port=" + port;
        return sendRequest(url, "PUT");
    }

    private static String deregister(String serviceName, String ip, int port) throws Exception {
        String url = SERVER_URL + "/nacos/v1/ns/instance?" +
                "serviceName=" + encode(serviceName) +
                "&ip=" + encode(ip) +
                "&port=" + port;
        return sendRequest(url, "DELETE");
    }

    private static String sendRequest(String urlStr, String method) throws Exception {
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
            return "[" + code + "] " + sb.toString();
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String formatJson(String json) {
        if (json.length() > 150) {
            return json.substring(0, 150) + "...";
        }
        return json;
    }
}
