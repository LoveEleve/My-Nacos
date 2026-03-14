/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2;

import com.mynacos.naming.core.v2.pojo.ServiceInfo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * ServiceInfoHolder - 客户端本地缓存管理器
 *
 * 问题：服务端故障时，客户端如何继续提供服务发现？
 *
 * 方案：本地缓存 + 故障转移
 * 1. 内存缓存：ConcurrentHashMap<String, ServiceInfo>
 * 2. 磁盘备份：定期写入文件
 * 3. 更新机制：服务端推送时更新本地缓存
 * 4. 故障转移：服务端不可用时，使用本地缓存
 *
 * 对照：com.alibaba.nacos.client.naming.cache.ServiceInfoHolder
 */
public class ServiceInfoHolder {

    /**
     * 内存缓存：key = groupName@@serviceName@@clusters
     */
    private final Map<String, ServiceInfo> serviceInfoMap;

    /**
     * 故障转移目录
     */
    private final File failoverDir;

    /**
     * 更新回调（用于通知监听器）
     */
    private final CopyOnWriteArrayList<ServiceInfoUpdateListener> listeners;

    /**
     * 异步执行器（用于磁盘写入）
     */
    private final Executor executor;

    /**
     * 是否开启故障转移模式
     */
    private volatile boolean failoverMode = false;

    public ServiceInfoHolder(String cacheDir) {
        this.serviceInfoMap = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ServiceInfoHolder-Writer");
            t.setDaemon(true);
            return t;
        });

        // 初始化故障转移目录
        this.failoverDir = new File(cacheDir, "failover");
        if (!failoverDir.exists()) {
            failoverDir.mkdirs();
        }

        // 加载磁盘缓存
        loadFailoverData();
    }

    /**
     * 处理服务信息更新（服务端推送或查询返回）
     *
     * @param serviceInfo 新的服务信息
     * @return 是否更新成功
     */
    public boolean processServiceInfo(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return false;
        }

        String key = serviceInfo.getKey();
        ServiceInfo old = serviceInfoMap.get(key);

        // 判断是否更新（通过 lastRefTime）
        if (old == null || old.getLastRefTime() < serviceInfo.getLastRefTime()) {
            serviceInfoMap.put(key, serviceInfo);

            System.out.println("[ServiceInfoHolder] Updated: " + key
                + " with " + (serviceInfo.getHosts() != null ? serviceInfo.getHosts().size() : 0) + " hosts");

            // 异步写入磁盘
            writeToDisk(serviceInfo);

            // 通知监听器
            notifyListeners(serviceInfo);

            return true;
        }

        // 没有变化，只刷新引用时间
        old.refreshRefTime();
        return false;
    }

    /**
     * 获取服务信息
     *
     * @param name       服务名
     * @param groupName  分组名
     * @param clusters   集群名
     * @return ServiceInfo，如果不存在返回 null
     */
    public ServiceInfo getServiceInfo(String name, String groupName, String clusters) {
        String key = ServiceInfo.getKey(name, groupName, clusters);
        ServiceInfo info = serviceInfoMap.get(key);

        if (info != null) {
            info.refreshRefTime(); // 刷新引用时间
        }

        return info;
    }

    /**
     * 获取服务信息（故障转移模式）
     *
     * @param name       服务名
     * @param groupName  分组名
     * @param clusters   集群名
     * @return ServiceInfo，如果缓存过期则返回 null
     */
    public ServiceInfo getServiceInfoSafe(String name, String groupName, String clusters) {
        ServiceInfo info = getServiceInfo(name, groupName, clusters);

        // 故障转移模式下，即使过期也返回
        if (failoverMode) {
            return info;
        }

        // 正常模式下，过期返回 null
        if (info != null && info.isExpired()) {
            return null;
        }

        return info;
    }

    /**
     * 清除服务缓存
     */
    public void clearService(String name, String groupName, String clusters) {
        String key = ServiceInfo.getKey(name, groupName, clusters);
        serviceInfoMap.remove(key);

        // 删除磁盘文件
        File file = new File(failoverDir, key + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 进入故障转移模式
     */
    public void enterFailoverMode() {
        if (!failoverMode) {
            failoverMode = true;
            System.out.println("[ServiceInfoHolder] Entered FAILOVER mode");

            // 从磁盘加载所有缓存
            loadFailoverData();
        }
    }

    /**
     * 退出故障转移模式
     */
    public void exitFailoverMode() {
        if (failoverMode) {
            failoverMode = false;
            System.out.println("[ServiceInfoHolder] Exited FAILOVER mode");
        }
    }

    /**
     * 是否处于故障转移模式
     */
    public boolean isFailoverMode() {
        return failoverMode;
    }

    /**
     * 添加更新监听器
     */
    public void addListener(ServiceInfoUpdateListener listener) {
        listeners.add(listener);
    }

    /**
     * 移除更新监听器
     */
    public void removeListener(ServiceInfoUpdateListener listener) {
        listeners.remove(listener);
    }

    /**
     * 通知所有监听器
     */
    private void notifyListeners(ServiceInfo serviceInfo) {
        for (ServiceInfoUpdateListener listener : listeners) {
            try {
                listener.onUpdate(serviceInfo);
            } catch (Exception e) {
                System.err.println("[ServiceInfoHolder] Listener error: " + e.getMessage());
            }
        }
    }

    /**
     * 异步写入磁盘
     */
    private void writeToDisk(ServiceInfo serviceInfo) {
        executor.execute(() -> {
            try {
                String key = serviceInfo.getKey();
                File file = new File(failoverDir, key + ".json");

                // 简单 JSON 序列化
                String json = toJson(serviceInfo);

                try (FileOutputStream fos = new FileOutputStream(file);
                     OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                    writer.write(json);
                }

                System.out.println("[ServiceInfoHolder] Written to disk: " + key);
            } catch (Exception e) {
                System.err.println("[ServiceInfoHolder] Write failed: " + e.getMessage());
            }
        });
    }

    /**
     * 从磁盘加载故障转移数据
     */
    private void loadFailoverData() {
        File[] files = failoverDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                ServiceInfo info = loadFromFile(file);
                if (info != null) {
                    serviceInfoMap.put(info.getKey(), info);
                    System.out.println("[ServiceInfoHolder] Loaded from disk: " + info.getKey());
                }
            } catch (Exception e) {
                System.err.println("[ServiceInfoHolder] Load failed: " + e.getMessage());
            }
        }
    }

    /**
     * 从文件加载 ServiceInfo
     */
    private ServiceInfo loadFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            // 简单 JSON 解析（实际应该用 JSON 库）
            return parseJson(sb.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 简单 JSON 序列化（实际应该用 Jackson/Gson）
     */
    private String toJson(ServiceInfo info) {
        // 简化实现，直接返回 key
        return "{\"key\":\"" + info.getKey() + "\",\"name\":\"" + info.getName() + "\"}";
    }

    /**
     * 简单 JSON 解析
     */
    private ServiceInfo parseJson(String json) {
        // 简化实现
        ServiceInfo info = new ServiceInfo();
        // 从文件名解析 key
        return info;
    }

    /**
     * 服务信息更新监听器接口
     */
    public interface ServiceInfoUpdateListener {
        void onUpdate(ServiceInfo serviceInfo);
    }
}
