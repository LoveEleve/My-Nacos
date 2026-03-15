/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.consistency.distro;

import com.mynacos.naming.core.v2.ConsistentHashRing;
import com.mynacos.naming.core.v2.ServerMemberManager;
import com.mynacos.naming.core.v2.pojo.Member;
import com.mynacos.naming.core.v2.pojo.Service;

import java.util.List;
import java.util.concurrent.*;

/**
 * DistroProtocol - Distro 一致性协议核心
 *
 * 问题：多节点如何保证数据最终一致？
 *
 * 方案：Distro 协议（AP）
 * 1. 每个服务由特定节点负责（一致性哈希）
 * 2. 写操作：转发到负责节点，立即返回
 * 3. 同步：异步广播给其他节点
 * 4. 读操作：直接读本地（可能读到旧数据）
 *
 * 对照：com.alibaba.nacos.naming.consistency.ephemeral.distro.v2.DistroProtocol
 */
public class DistroProtocol {

    /**
     * 集群成员管理器
     */
    private final ServerMemberManager memberManager;

    /**
     * 一致性哈希环（决定数据归属）
     */
    private final ConsistentHashRing hashRing;

    /**
     * 同步任务线程池
     */
    private final ScheduledExecutorService syncExecutor;

    /**
     * 是否运行中
     */
    private volatile boolean running = false;

    public DistroProtocol() {
        this.memberManager = ServerMemberManager.getInstance();
        this.hashRing = new ConsistentHashRing();

        this.syncExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Distro-Sync");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 启动 Distro 协议
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;

        // 初始化哈希环
        refreshHashRing();

        // 启动定时同步任务（每 5 秒）
        syncExecutor.scheduleAtFixedRate(this::syncToOthers, 5, 5, TimeUnit.SECONDS);

        System.out.println("[DistroProtocol] Started");
    }

    /**
     * 停止 Distro 协议
     */
    public void stop() {
        running = false;
        syncExecutor.shutdown();
        System.out.println("[DistroProtocol] Stopped");
    }

    /**
     * 判断是否是当前节点负责的数据
     *
     * @param service 服务
     * @return true=当前节点负责
     */
    public boolean isResponsible(Service service) {
        if (hashRing.isEmpty()) {
            return true; // 单节点模式
        }

        Member responsible = hashRing.getResponsibleMember(service.getName());
        if (responsible == null) {
            return true;
        }

        return memberManager.isSelf(responsible);
    }

    /**
     * 获取负责该服务的节点
     */
    public Member getResponsibleMember(Service service) {
        return hashRing.getResponsibleMember(service.getName());
    }

    /**
     * 添加数据（如果是本节点负责）
     */
    public boolean onPut(Service service, byte[] data) {
        if (!isResponsible(service)) {
            // 不是本节点负责，转发到负责节点
            Member target = getResponsibleMember(service);
            System.out.println("[DistroProtocol] Forward to " + target.getAddress() + " for " + service.getName());
            // TODO: 实际转发
            return false;
        }

        // 是本节点负责，存储数据
        System.out.println("[DistroProtocol] Store locally: " + service.getName());
        // TODO: 实际存储

        // 触发异步同步
        syncToOthers(service, data);

        return true;
    }

    /**
     * 接收其他节点同步的数据
     */
    public void onSyncData(Service service, byte[] data) {
        System.out.println("[DistroProtocol] Receive sync data for: " + service.getName());
        // TODO: 实际存储
    }

    /**
     * 刷新哈希环
     */
    public void refreshHashRing() {
        // 清空旧的
        for (Member member : hashRing.getAllMembers()) {
            hashRing.removeMember(member);
        }

        // 添加所有健康节点
        List<Member> healthyMembers = memberManager.getAllHealthyMembers();
        for (Member member : healthyMembers) {
            hashRing.addMember(member);
        }

        System.out.println("[DistroProtocol] Hash ring refreshed with " + hashRing.getMemberCount() + " members");
    }

    /**
     * 同步数据给其他节点（全量同步）
     */
    private void syncToOthers() {
        if (!running) {
            return;
        }

        List<Member> healthyMembers = memberManager.getHealthyMembers();
        if (healthyMembers.isEmpty()) {
            return;
        }

        // TODO: 获取本节点负责的所有数据，同步给其他节点
        System.out.println("[DistroProtocol] Syncing data to " + healthyMembers.size() + " members");
    }

    /**
     * 同步特定服务给其他节点
     */
    private void syncToOthers(Service service, byte[] data) {
        if (!running) {
            return;
        }

        // 广播给所有其他健康节点
        List<Member> healthyMembers = memberManager.getHealthyMembers();
        for (Member member : healthyMembers) {
            syncToTarget(member, service, data);
        }
    }

    /**
     * 同步到目标节点
     */
    private void syncToTarget(Member target, Service service, byte[] data) {
        System.out.println("[DistroProtocol] Sync " + service.getName() + " to " + target.getAddress());
        // TODO: 实际网络发送
    }

    /**
     * 节点加入时调用
     */
    public void onMemberJoin(Member member) {
        System.out.println("[DistroProtocol] Member joined: " + member.getAddress());
        hashRing.addMember(member);

        // 触发数据同步给新节点
        // TODO: 全量同步本节点负责的数据给新节点
    }

    /**
     * 节点离开时调用
     */
    public void onMemberLeave(Member member) {
        System.out.println("[DistroProtocol] Member left: " + member.getAddress());
        hashRing.removeMember(member);
    }
}
