/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Nacos HTTP 服务器
 *
 * 基于 JDK HttpServer 实现简单的 HTTP 接口
 */
public class NacosHttpServer {

    private final int port;
    private final InstanceController instanceController;
    private HttpServer server;

    public NacosHttpServer(int port) {
        this.port = port;
        this.instanceController = new InstanceController();
    }

    /**
     * 启动服务器
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // 注册接口
        server.createContext("/nacos/v1/ns/instance", new InstanceHandler());
        // 查询接口
        server.createContext("/nacos/v1/ns/instance/list", new ListHandler());
        // 心跳接口
        server.createContext("/nacos/v1/ns/instance/beat", new BeatHandler());

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("[NacosHttpServer] Started on port " + port);
    }

    /**
     * 停止服务器
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[NacosHttpServer] Stopped");
        }
    }

    /**
     * 实例注册/注销处理器
     */
    class InstanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());

            String response;
            int statusCode = 200;

            try {
                String namespaceId = params.getOrDefault("namespaceId", "public");
                String groupName = params.getOrDefault("groupName", "DEFAULT_GROUP");
                String serviceName = params.get("serviceName");
                String ip = params.get("ip");
                String portStr = params.get("port");

                if (serviceName == null || ip == null || portStr == null) {
                    response = "{\"code\":400,\"message\":\"Missing required parameters\"}";
                    statusCode = 400;
                } else {
                    int port = Integer.parseInt(portStr);
                    double weight = Double.parseDouble(params.getOrDefault("weight", "1.0"));
                    boolean healthy = Boolean.parseBoolean(params.getOrDefault("healthy", "true"));

                    switch (method) {
                        case "POST":
                            // 注册
                            response = instanceController.registerInstance(
                                namespaceId, groupName, serviceName, ip, port,
                                weight, healthy, params);
                            break;
                        case "DELETE":
                            // 注销
                            response = instanceController.deregisterInstance(
                                namespaceId, groupName, serviceName, ip, port);
                            break;
                        default:
                            response = "{\"code\":405,\"message\":\"Method not allowed\"}";
                            statusCode = 405;
                    }
                }
            } catch (Exception e) {
                response = "{\"code\":500,\"message\":\"" + e.getMessage() + "\"}";
                statusCode = 500;
            }

            sendResponse(exchange, statusCode, response);
        }
    }

    /**
     * 列表查询处理器
     */
    class ListHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if (!"GET".equals(method)) {
                sendResponse(exchange, 405, "{\"code\":405,\"message\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());

            String namespaceId = params.getOrDefault("namespaceId", "public");
            String groupName = params.getOrDefault("groupName", "DEFAULT_GROUP");
            String serviceName = params.get("serviceName");

            if (serviceName == null) {
                sendResponse(exchange, 400, "{\"code\":400,\"message\":\"Missing serviceName\"}");
                return;
            }

            String response = instanceController.listInstances(namespaceId, groupName, serviceName);
            sendResponse(exchange, 200, response);
        }
    }

    /**
     * 心跳处理器
     */
    class BeatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if (!"PUT".equals(method)) {
                sendResponse(exchange, 405, "{\"code\":405,\"message\":\"Method not allowed\"}");
                return;
            }

            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());

            String namespaceId = params.getOrDefault("namespaceId", "public");
            String groupName = params.getOrDefault("groupName", "DEFAULT_GROUP");
            String serviceName = params.get("serviceName");
            String ip = params.get("ip");
            String portStr = params.get("port");

            if (serviceName == null || ip == null || portStr == null) {
                sendResponse(exchange, 400, "{\"code\":400,\"message\":\"Missing required parameters\"}");
                return;
            }

            int port = Integer.parseInt(portStr);
            String response = instanceController.beat(namespaceId, groupName, serviceName, ip, port);
            sendResponse(exchange, 200, response);
        }
    }

    /**
     * 解析查询参数
     */
    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                result.put(kv[0], java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    /**
     * 发送 HTTP 响应
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void main(String[] args) throws IOException {
        NacosHttpServer server = new NacosHttpServer(8848);
        server.start();

        System.out.println("\nPress Enter to stop...");
        System.in.read();

        server.stop();
    }
}
