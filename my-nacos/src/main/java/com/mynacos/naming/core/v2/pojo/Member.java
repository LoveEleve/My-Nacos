/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2.pojo;

import java.io.Serializable;
import java.util.Objects;

/**
 * Member - 集群节点
 *
 * 问题：如何表示集群中的一个节点？
 *
 * 属性：
 * - ip: 节点 IP
 * - port: 节点端口
 * - state: 节点状态（UP/DOWN/SUSPICIOUS）
 * - lastActiveTime: 最后活跃时间（用于健康检查）
 *
 * 对照：com.alibaba.nacos.core.cluster.Member
 */
public class Member implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点 IP
     */
    private String ip;

    /**
     * 节点端口
     */
    private int port;

    /**
     * 节点状态
     */
    private NodeState state = NodeState.UP;

    /**
     * 最后活跃时间
     */
    private long lastActiveTime;

    /**
     * 节点角色（如：FOLLOWER/LEADER，Distro 中暂时不用）
     */
    private String role;

    public Member() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    public Member(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 获取节点地址（ip:port）
     */
    public String getAddress() {
        return ip + ":" + port;
    }

    /**
     * 更新最后活跃时间
     */
    public void updateActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 判断是否健康（15秒内活跃过）
     */
    public boolean isHealthy() {
        return state == NodeState.UP
                && System.currentTimeMillis() - lastActiveTime < 15000;
    }

    // ==================== Getters & Setters ====================

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public NodeState getState() {
        return state;
    }

    public void setState(NodeState state) {
        this.state = state;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // ==================== equals & hashCode ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member)) return false;
        Member member = (Member) o;
        return port == member.port && Objects.equals(ip, member.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    @Override
    public String toString() {
        return "Member{" + "ip='" + ip + '\'' + ", port=" + port + ", state=" + state + '}';
    }

    /**
     * 节点状态枚举
     */
    public enum NodeState {
        UP,         // 健康
        DOWN,       // 下线
        SUSPICIOUS  // 可疑（疑似不健康）
    }
}
