/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2.pojo;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ServiceInfo - 服务信息（客户端缓存用）
 *
 * 问题：客户端如何缓存服务实例列表？
 *
 * 方案：ServiceInfo 封装服务的完整信息
 * - 包含服务名、分组、集群、实例列表
 * - 可序列化到磁盘（故障转移）
 * - 通过 lastRefTime 判断缓存新鲜度
 *
 * 对照：com.alibaba.nacos.api.naming.pojo.ServiceInfo
 */
public class ServiceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 服务名
     */
    private String name;

    /**
     * 分组名
     */
    private String groupName;

    /**
     * 集群名
     */
    private String clusters;

    /**
     * 缓存时间（毫秒）
     */
    private long cacheMillis;

    /**
     * 最后引用时间（用于判断缓存是否过期）
     */
    private long lastRefTime;

    /**
     * 实例列表
     */
    private List<InstancePublishInfo> hosts;

    public ServiceInfo() {
        this.cacheMillis = 10000; // 默认10秒
    }

    /**
     * 获取缓存 key
     * 格式：name@@groupName@@clusters
     */
    public String getKey() {
        return getKey(name, groupName, clusters);
    }

    /**
     * 静态方法生成 key
     */
    public static String getKey(String name, String groupName, String clusters) {
        StringBuilder sb = new StringBuilder();
        sb.append(groupName).append("@@").append(name);
        if (clusters != null && !clusters.isEmpty()) {
            sb.append("@").append(clusters);
        }
        return sb.toString();
    }

    /**
     * 判断缓存是否过期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() - lastRefTime > cacheMillis;
    }

    /**
     * 刷新引用时间
     */
    public void refreshRefTime() {
        this.lastRefTime = System.currentTimeMillis();
    }

    // ==================== Getters & Setters ====================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getClusters() {
        return clusters;
    }

    public void setClusters(String clusters) {
        this.clusters = clusters;
    }

    public long getCacheMillis() {
        return cacheMillis;
    }

    public void setCacheMillis(long cacheMillis) {
        this.cacheMillis = cacheMillis;
    }

    public long getLastRefTime() {
        return lastRefTime;
    }

    public void setLastRefTime(long lastRefTime) {
        this.lastRefTime = lastRefTime;
    }

    public List<InstancePublishInfo> getHosts() {
        return hosts;
    }

    public void setHosts(List<InstancePublishInfo> hosts) {
        this.hosts = hosts;
    }

    /**
     * 获取健康的实例列表
     */
    public List<InstancePublishInfo> getHealthyHosts() {
        if (hosts == null) {
            return null;
        }
        return hosts.stream()
                .filter(InstancePublishInfo::isHealthy)
                .filter(InstancePublishInfo::isEnabled)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "ServiceInfo{" +
                "name='" + name + '\'' +
                ", groupName='" + groupName + '\'' +
                ", hosts=" + (hosts != null ? hosts.size() : 0) +
                ", expired=" + isExpired() +
                '}';
    }
}
