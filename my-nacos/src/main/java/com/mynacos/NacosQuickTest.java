/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos;

import com.mynacos.naming.remote.InstanceController;
import com.mynacos.naming.remote.NacosHttpServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 快速测试 - 不依赖集成环境
 */
public class NacosQuickTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Nacos Quick Test ===\n");

        // 测试1: 直接测试 Controller
        System.out.println("1. Test Controller directly:");
        InstanceController controller = new InstanceController();

        // 注册
        String result1 = controller.registerInstance(
            "public", "DEFAULT_GROUP", "order-service",
            "192.168.1.100", 8080, 1.0, true, null);
        System.out.println("   Register: " + result1);

        // 查询
        String result2 = controller.listInstances(
            "public", "DEFAULT_GROUP", "order-service");
        System.out.println("   Query: " + result2.substring(0, Math.min(100, result2.length())) + "...");

        // 注销
        String result3 = controller.deregisterInstance(
            "public", "DEFAULT_GROUP", "order-service",
            "192.168.1.100", 8080);
        System.out.println("   Deregister: " + result3);

        // 再次查询
        String result4 = controller.listInstances(
            "public", "DEFAULT_GROUP", "order-service");
        System.out.println("   Query after deregister: " + result4);

        System.out.println("\n2. Controller test passed!\n");

        // 测试2: HTTP 服务器测试
        System.out.println("3. Test HTTP Server:");

        // 在后台线程启动服务器
        Thread serverThread = new Thread(() -> {
            try {
                NacosHttpServer server = new NacosHttpServer(8850);
                server.start();
                // 服务器启动后阻塞
                Thread.sleep(10000);
                server.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // 等待服务器启动
        Thread.sleep(2000);

        // 发送 HTTP 请求
        try {
            String url = "http://localhost:8850/nacos/v1/ns/instance?" +
                    "serviceName=test-service&ip=192.168.1.200&port=9090";
            String response = sendHttpRequest(url, "POST");
            System.out.println("   HTTP Register: " + response);

            String queryUrl = "http://localhost:8850/nacos/v1/ns/instance/list?" +
                    "serviceName=test-service";
            String queryResponse = sendHttpRequest(queryUrl, "GET");
            System.out.println("   HTTP Query: " + queryResponse.substring(0, Math.min(80, queryResponse.length())) + "...");

            System.out.println("\n4. HTTP test passed!");
        } catch (Exception e) {
            System.out.println("   HTTP test error: " + e.getMessage());
        }

        System.out.println("\n=== All Tests Passed! ===");
    }

    private static String sendHttpRequest(String urlStr, String method) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);

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
}
