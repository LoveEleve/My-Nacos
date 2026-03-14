package com.mynacos.client;

/**
 * 客户端使用示例
 */
public class ClientDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== My Nacos Client Demo ===\n");
        
        // 创建客户端
        NacosClient client = new NacosClient("localhost:8848");
        
        // 1. 注册服务实例
        System.out.println("1. 注册服务实例...");
        boolean registered = client.registerInstance(
            "order-service", 
            "192.168.1.100", 
            8080
        );
        System.out.println("注册结果: " + (registered ? "成功" : "失败") + "\n");
        
        // 2. 查询服务实例
        System.out.println("2. 查询服务实例...");
        Thread.sleep(1000);  // 等待注册完成
        String instances = client.getInstances("order-service");
        System.out.println("查询结果: " + instances + "\n");
        
        // 3. 保持运行，发送心跳
        System.out.println("3. 心跳发送中（每5秒一次），按 Enter 键停止...\n");
        System.in.read();
        
        // 关闭客户端
        client.shutdown();
        System.out.println("客户端已关闭");
    }
}
