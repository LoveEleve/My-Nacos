/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.push;

import com.mynacos.naming.core.v2.pojo.Service;

import java.util.Map;
import java.util.concurrent.*;

/**
 * PushDelayTaskExecuteEngine - 推送延迟任务执行引擎
 *
 * 问题：如何管理大量延迟推送任务？
 *
 * 方案：
 * - 内存队列存储任务
 * - 每 100ms 扫描一次
 * - 到期任务提交给线程池执行
 * - 新任务合并旧任务（避免重复推送）
 *
 * 对照：com.alibaba.nacos.naming.push.v2.PushDelayTaskExecuteEngine
 */
public class PushDelayTaskExecuteEngine {

    /**
     * 扫描间隔：100ms
     */
    private static final long SCAN_INTERVAL = 100;

    /**
     * 任务存储：Service → DelayTask
     */
    private final Map<Service, DelayTask> tasks;

    /**
     * 任务执行线程池
     */
    private final ExecutorService executor;

    /**
     * 调度线程
     */
    private final ScheduledExecutorService scheduler;

    /**
     * 任务处理器
     */
    private final PushTaskProcessor processor;

    /**
     * 是否运行中
     */
    private volatile boolean running = false;

    public PushDelayTaskExecuteEngine(PushTaskProcessor processor) {
        this.tasks = new ConcurrentHashMap<>();
        this.processor = processor;
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "Push-Executor");
            t.setDaemon(true);
            return t;
        });
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Push-Scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 启动引擎
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;

        // 每 100ms 扫描一次
        scheduler.scheduleAtFixedRate(this::processTasks, SCAN_INTERVAL, SCAN_INTERVAL, TimeUnit.MILLISECONDS);

        System.out.println("[PushEngine] Started");
    }

    /**
     * 停止引擎
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        executor.shutdown();
        System.out.println("[PushEngine] Stopped");
    }

    /**
     * 添加任务
     *
     * 核心逻辑：新任务合并旧任务
     */
    public void addTask(Service service, DelayTask newTask) {
        DelayTask existTask = tasks.get(service);
        if (existTask != null) {
            // 新任务合并旧任务（取并集）
            newTask.merge(existTask);
        }
        tasks.put(service, newTask);
    }

    /**
     * 扫描并处理到期任务
     */
    private void processTasks() {
        for (Map.Entry<Service, DelayTask> entry : tasks.entrySet()) {
            DelayTask task = entry.getValue();
            if (task.shouldProcess()) {
                // 从队列移除
                tasks.remove(entry.getKey(), task);

                // 提交执行
                executor.submit(() -> {
                    try {
                        processor.process((PushDelayTask) task);
                    } catch (Exception e) {
                        System.err.println("[PushEngine] Task failed: " + e.getMessage());
                    }
                });
            }
        }
    }
}
