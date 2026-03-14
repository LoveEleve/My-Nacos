/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.push;

/**
 * 推送任务处理器接口
 */
public interface PushTaskProcessor {

    /**
     * 处理推送任务
     * @param task 推送任务
     */
    void process(PushDelayTask task);
}
