package com.mynacos.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Nacos 服务端 - 阶段1：单机 MVP
 * 
 * 提供 HTTP 接口：
 * - POST /nacos/v1/ns/instance - 注册实例
 * - GET /nacos/v1/ns/instance/list - 查询实例列表
 * - PUT /nacos/v1/ns/instance/beat - 心跳
 */
public class NacosServer {
    
    private final int port;
    private final ServiceManager serviceManager;
    private final HealthCheckProcessor healthCheckProcessor;
    private HttpServer server;
    
    public NacosServer(int port) {
        this.port = port;
        this.serviceManager = new ServiceManager();
        this.healthCheckProcessor = new HealthCheckProcessor(serviceManager);
    }
    
    /**
     * 启动服务端
     */
    public void start() throws IOException {
        // 创建 HTTP 服务器
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // 注册接口
        server.createContext("/nacos/v1/ns/instance", new RegisterHandler());
        // 查询接口
        server.createContext("/nacos/v1/ns/instance/list", new QueryHandler());
        // 心跳接口
        server.createContext("/nacos/v1/ns/instance/beat", new BeatHandler());
        
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        
        // 启动健康检查
        healthCheckProcessor.start();
        
        System.out.println("Nacos Server started on port " + port);
    }
    
    /**
     * 注册处理器
     */
    class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            // 解析参数
            String query = exchange.getRequestURI().getQuery();
            java.util.Map<String, String> params = parseQuery(query);
            
            String namespaceId = params.getOrDefault("namespaceId", "public");
            String groupName = params.getOrDefault("groupName", "DEFAULT_GROUP");
            String serviceName = params.get("serviceName");
            String ip = params.get("ip");
            int port = Integer.parseInt(params.getOrDefault("port", "0"));
            
            if (serviceName == null || ip == null || port == 0) {
                sendResponse(exchange, 400, "Missing required parameters");
                return;
            }
            
            // 创建实例
            Instance instance = new Instance(ip, port);
            instance.setMetadata(params);
            
            // 添加到服务管理器
            serviceManager.addInstance(namespaceId, groupName, serviceName, instance);
            
            System.out.println("Instance registered: " + serviceName + " -> " + ip + ":" + port);
            sendResponse(exchange, 200, "ok");
        }
    }
    
    /**
     * 查询处理器
     */
    class QueryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            // 解析参数
            String query = exchange.getRequestURI().getQuery();
            java.util.Map<String, String> params = parseQuery(query);
            
            String namespaceId = params.getOrDefault("namespaceId", "public");
            String groupName = params.getOrDefault("groupName", "DEFAULT_GROUP");
            String serviceName = params.get("serviceName");
            
            if (serviceName == null) {
                sendResponse(exchange, 400, "Missing serviceName");
                return;
            }
            
            // 查询实例列表
            java.util.List<Instance> instances = serviceManager.getInstances(
                namespaceId, groupName, serviceName);
            
            // 构建响应
            StringBuilder response = new StringBuilder();
            response.append("{");
            response.append("\"serviceName\":\"").append(serviceName).append("\",");
            response.append("\"hosts\":[");
            
            for (int i = 0; i < instances.size(); i++) {
                Instance inst = instances.get(i);
                if (i > 0) response.append(",");
                response.append("{");
                response.append("\"ip\":\"").append(inst.getIp()).append("\",");
                response.append("\"port\":").append(inst.getPort()).append(",");
                response.append("\"healthy\":").append(inst.isHealthy());
                response.append("}");
            }
            
            response.append("]}");
            
            sendResponse(exchange, 200, response.toString());
        }
    }
    
    /**
     * 心跳处理器
     */
    class BeatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"PUT".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            // 解析参数
            String query = exchange.getRequestURI().getQuery();
            java.util.Map<String, String> params = parseQuery(query);
            
            String namespaceId = params.getOrDefault("namespaceId", "public");
            String groupName = params.getOrDefault("groupName", "DEFAULT_GROUP");
            String serviceName = params.get("serviceName");
            String ip = params.get("ip");
            int port = Integer.parseInt(params.getOrDefault("port", "0"));
            
            if (serviceName == null || ip == null || port == 0) {
                sendResponse(exchange, 400, "Missing required parameters");
                return;
            }
            
            // 处理心跳
            healthCheckProcessor.processBeat(namespaceId, groupName, serviceName, ip, port);
            
            sendResponse(exchange, 200, "ok");
        }
    }
    
    /**
     * 解析查询参数
     */
    private java.util.Map<String, String> parseQuery(String query) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }
        
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                result.put(kv[0], kv[1]);
            }
        }
        return result;
    }
    
    /**
     * 发送 HTTP 响应
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) 
            throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    /**
     * 停止服务端
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
    
    public static void main(String[] args) throws IOException {
        NacosServer server = new NacosServer(8848);
        server.start();
        
        System.out.println("Press Enter to stop...");
        System.in.read();
        server.stop();
    }
}
