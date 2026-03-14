/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2.pojo;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service - 服务定义
 * 
 * 问题：如何唯一标识一个服务，并保证内存中只有一个实例？
 * 
 * 方案：不可变对象 + equals/hashCode 基于业务 key
 * - 不可变：创建后不能修改，线程安全
 * - equals：只比较 namespace + group + name
 * - 结果：相同三元组的对象 equals 相等，适合作为 Map key
 * 
 * 对照：com.alibaba.nacos.naming.core.v2.pojo.Service
 */
public class Service implements Serializable {

    private static final long serialVersionUID = -990509089519499344L;

    /**
     * 命名空间 - 隔离不同环境（如生产、测试）
     */
    private final String namespace;

    /**
     * 分组名 - 对服务进行逻辑分组
     */
    private final String group;

    /**
     * 服务名
     */
    private final String name;

    /**
     * 是否为临时实例
     * true=临时（需心跳保活），false=持久（需主动删除）
     */
    private final boolean ephemeral;

    /**
     * 版本号 - 每次变更时递增，用于 Distro 协议同步
     */
    private final AtomicLong revision;

    /**
     * 最后更新时间
     */
    private long lastUpdatedTime;

    /**
     * 私有构造器 - 强制使用工厂方法
     */
    private Service(String namespace, String group, String name, boolean ephemeral) {
        this.namespace = namespace;
        this.group = group;
        this.name = name;
        this.ephemeral = ephemeral;
        this.revision = new AtomicLong();
        this.lastUpdatedTime = System.currentTimeMillis();
    }

    /**
     * 创建临时服务实例（默认）
     */
    public static Service newService(String namespace, String group, String name) {
        return newService(namespace, group, name, true);
    }

    /**
     * 创建服务实例
     */
    public static Service newService(String namespace, String group, String name, boolean ephemeral) {
        return new Service(namespace, group, name, ephemeral);
    }

    // ==================== Getters ====================

    public String getNamespace() { return namespace; }
    public String getGroup() { return group; }
    public String getName() { return name; }
    public boolean isEphemeral() { return ephemeral; }
    public long getRevision() { return revision.get(); }
    public long getLastUpdatedTime() { return lastUpdatedTime; }

    // ==================== 业务方法 ====================

    /**
     * 更新最后更新时间
     */
    public void renewUpdateTime() {
        this.lastUpdatedTime = System.currentTimeMillis();
    }

    /**
     * 递增版本号 - 服务变更时调用
     */
    public void incrementRevision() {
        revision.incrementAndGet();
    }

    /**
     * 获取带分组的服务名
     * 格式：groupName@@serviceName
     */
    public String getGroupedServiceName() {
        if ("DEFAULT_GROUP".equals(group)) {
            return name;
        }
        return group + "@@" + name;
    }

    // ==================== equals & hashCode ====================
    
    /**
     * 关键：只比较 namespace + group + name
     * 这使得相同三元组的对象 equals 相等，可作为 Map key
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Service)) return false;
        Service service = (Service) o;
        return namespace.equals(service.namespace) 
            && group.equals(service.group) 
            && name.equals(service.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, group, name);
    }

    @Override
    public String toString() {
        return "Service{" + "namespace='" + namespace + '\'' 
            + ", group='" + group + '\'' 
            + ", name='" + name + '\'' + '}';
    }
}
