/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.push;

import com.mynacos.naming.core.v2.pojo.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PushDelayTask - 推送延迟任务
 *
 * 问题：服务频繁变更时，如何减少推送次数？
 *
 * 方案：延迟合并
 * - 延迟 500ms 执行
 * - 同一 Service 的多次变更合并为一次推送
 * - 新任务合并旧任务（取并集）
 *
 * 对照：com.alibaba.nacos.naming.push.v2.task.PushDelayTask
 */
public class PushDelayTask implements DelayTask {

    /**
     * 延迟时间：500ms
     */
    public static final long DELAY_TIME = 500;

    /**
     * 任务唯一标识（Service）
     */
    private final Service service;

    /**
     * 是否推送给所有订阅者
     * true：推送给所有订阅者（服务变更时）
     * false：只推送给指定客户端（新客户端订阅时）
     */
    private boolean pushToAll;

    /**
     * 指定推送的客户端集合（pushToAll=false 时使用）
     */
    private Set<String> targetClients;

    /**
     * 最后处理时间（用于延迟计算）
     */
    private long lastProcessTime;

    /**
     * 创建全量推送任务
     */
    public PushDelayTask(Service service) {
        this.service = service;
        this.pushToAll = true;
        this.lastProcessTime = System.currentTimeMillis();
    }

    /**
     * 创建指定客户端推送任务
     */
    public PushDelayTask(Service service, String clientId) {
        this.service = service;
        this.pushToAll = false;
        this.targetClients = ConcurrentHashMap.newKeySet();
        this.targetClients.add(clientId);
        this.lastProcessTime = System.currentTimeMillis();
    }

    @Override
    public Service getTaskKey() {
        return service;
    }

    @Override
    public long getLastProcessTime() {
        return lastProcessTime;
    }

    @Override
    public void setLastProcessTime(long time) {
        this.lastProcessTime = time;
    }

    @Override
    public boolean shouldProcess() {
        return System.currentTimeMillis() - lastProcessTime >= DELAY_TIME;
    }

    @Override
    public void merge(DelayTask oldTask) {
        if (!(oldTask instanceof PushDelayTask)) {
            return;
        }

        PushDelayTask oldPushTask = (PushDelayTask) oldTask;

        // 合并策略：
        // 1. 如果任意一方是全量推送，合并后为全量推送
        // 2. 如果都是单客户端推送，合并目标集合
        if (this.pushToAll || oldPushTask.pushToAll) {
            this.pushToAll = true;
            this.targetClients = null;
        } else {
            this.targetClients.addAll(oldPushTask.targetClients);
        }

        // 取更早的处理时间（保证不会无限延迟）
        this.lastProcessTime = Math.min(this.lastProcessTime, oldPushTask.lastProcessTime);

        System.out.println("[PushDelayTask] Merged for " + service.getName());
    }

    // ==================== Getters ====================

    public Service getService() {
        return service;
    }

    public boolean isPushToAll() {
        return pushToAll;
    }

    public Set<String> getTargetClients() {
        return targetClients;
    }

    @Override
    public String toString() {
        return "PushDelayTask{" + "service=" + service.getName()
            + ", pushToAll=" + pushToAll
            + ", delay=" + (System.currentTimeMillis() - lastProcessTime) + "ms" + '}';
    }
}
