package com.mynacos.server;

/**
 * 服务端启动示例
 */
public class ServerDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== My Nacos Server Demo ===\n");
        
        // 创建并启动服务端
        NacosServer server = new NacosServer(8848);
        server.start();
        
        System.out.println("\n服务端已启动，按 Enter 键停止...");
        System.in.read();
        
        server.stop();
        System.out.println("服务端已停止");
    }
}
