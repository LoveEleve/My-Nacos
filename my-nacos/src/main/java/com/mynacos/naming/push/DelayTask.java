/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.push;

import com.mynacos.naming.core.v2.pojo.Service;

/**
 * 延迟任务接口
 */
public interface DelayTask {

    /**
     * 获取任务唯一标识（用于合并）
     */
    Service getTaskKey();

    /**
     * 获取最后处理时间
     */
    long getLastProcessTime();

    /**
     * 设置最后处理时间
     */
    void setLastProcessTime(long time);

    /**
     * 是否到期需要处理
     */
    boolean shouldProcess();

    /**
     * 合并旧任务
     * @param oldTask 旧任务
     */
    void merge(DelayTask oldTask);
}
