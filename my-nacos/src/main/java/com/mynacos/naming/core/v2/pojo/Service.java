/*
 * Copyright 2024 My-Nacos Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.mynacos.naming.core.v2.pojo;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service POJO for Nacos v2.
 *
 * 对照源码: com.alibaba.nacos.naming.core.v2.pojo.Service
 *
 * 设计要点:
 * 1. Service 是不可变对象 - 适合作为 Map 的 key
 * 2. equals/hashCode 只比较 namespace + group + name
 * 3. revision 用于版本控制(Distro同步)
 * 4. 静态工厂方法 newService()
 *
 * @author xiweng.yy (原始作者)
 */
public class Service implements Serializable {

    private static final long serialVersionUID = -990509089519499344L;

    /**
     * 命名空间 - final 不可变
     */
    private final String namespace;

    /**
     * 分组名 - final 不可变
     */
    private final String group;

    /**
     * 服务名 - final 不可变
     */
    private final String name;

    /**
     * 是否为临时实例 - final 不可变
     */
    private final boolean ephemeral;

    /**
     * 版本号 - 用于一致性同步
     * 每次服务变更时递增
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
     * @param ephemeral true=临时实例, false=持久实例
     */
    public static Service newService(String namespace, String group, String name, boolean ephemeral) {
        return new Service(namespace, group, name, ephemeral);
    }

    // ==================== Getters ====================

    public String getNamespace() {
        return namespace;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public long getRevision() {
        return revision.get();
    }

    public long getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    // ==================== 业务方法 ====================

    /**
     * 更新最后更新时间
     */
    public void renewUpdateTime() {
        this.lastUpdatedTime = System.currentTimeMillis();
    }

    /**
     * 递增版本号
     * 在 Distro 同步时使用
     */
    public void incrementRevision() {
        revision.incrementAndGet();
    }

    /**
     * 获取带分组的服务名
     * 格式: groupName@@serviceName
     */
    public String getGroupedServiceName() {
        if ("DEFAULT_GROUP".equals(group)) {
            return name;
        }
        return group + "@@" + name;
    }

    // ==================== 重写 equals/hashCode ====================

    /**
     * 关键: 只比较 namespace + group + name
     * 这使得 Service 可以作为 Map 的 key
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Service)) {
            return false;
        }
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
        return "Service{"
                + "namespace='" + namespace + '\''
                + ", group='" + group + '\''
                + ", name='" + name + '\''
                + ", ephemeral=" + ephemeral
                + ", revision=" + revision
                + '}';
    }
}
