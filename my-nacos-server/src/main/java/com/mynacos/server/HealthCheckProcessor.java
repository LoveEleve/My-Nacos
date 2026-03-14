package com.mynacos.server;

import java.util.concurrent.*;

/**
 * 健康检查处理器 - 第4章：怎么知道服务还活着？
 * 
 * 问题：服务实例宕机了，注册中心怎么知道？
 * 解决方案：心跳检测（客户端每5秒心跳，服务端15秒标记不健康，30秒剔除）
 */
public class HealthCheckProcessor implements Runnable {
    
    private final ServiceManager serviceManager;
    
    // 15秒无心跳，标记为不健康
    private static final long HEALTHY_TIMEOUT = 15 * 1000;
    
    // 30秒无心跳，剔除实例
    private static final long EXPIRE_TIMEOUT = 30 * 1000;
    
    private final ScheduledExecutorService executor = 
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HealthCheck");
            t.setDaemon(true);
            return t;
        });
    
    public HealthCheckProcessor(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }
    
    /**
     * 启动健康检查任务（每5秒执行一次）
     */
    public void start() {
        executor.scheduleWithFixedDelay(this, 5, 5, TimeUnit.SECONDS);
    }
    
    @Override
    public void run() {
        try {
            checkHealth();
        } catch (Exception e) {
            System.err.println("Health check failed: " + e.getMessage());
        }
    }
    
    /**
     * 检查所有服务的健康状态
     */
    private void checkHealth() {
        long currentTime = System.currentTimeMillis();
        
        for (Service service : serviceManager.getAllServices()) {
            for (Instance instance : service.getInstances()) {
                long diff = currentTime - instance.getLastBeatTime();
                
                // 超过15秒，标记不健康
                if (diff > HEALTHY_TIMEOUT && instance.isHealthy()) {
                    instance.setHealthy(false);
                    System.out.println("Instance unhealthy: " + instance.getIp() + ":" + instance.getPort());
                }
                
                // 超过30秒，剔除
                if (diff > EXPIRE_TIMEOUT) {
                    service.removeInstance(instance);
                    System.out.println("Instance removed: " + instance.getIp() + ":" + instance.getPort());
                }
            }
        }
    }
    
    /**
     * 处理客户端心跳
     */
    public void processBeat(String namespaceId, String groupName, 
                           String serviceName, String ip, int port) {
        // 查找服务
        for (Service service : serviceManager.getAllServices()) {
            if (service.getName().equals(serviceName)) {
                Instance instance = service.findInstance(ip, port);
                if (instance != null) {
                    // 更新心跳时间
                    instance.setLastBeatTime(System.currentTimeMillis());
                    
                    // 如果之前不健康，恢复为健康
                    if (!instance.isHealthy()) {
                        instance.setHealthy(true);
                        System.out.println("Instance healthy again: " + ip + ":" + port);
                    }
                    return;
                }
            }
        }
    }
}
