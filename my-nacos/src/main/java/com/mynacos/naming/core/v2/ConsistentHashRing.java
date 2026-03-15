/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2;

import com.mynacos.naming.core.v2.pojo.Member;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * ConsistentHashRing - 一致性哈希环
 *
 * 问题：如何决定哪个节点负责存储某个服务的数据？
 *
 * 方案：一致性哈希
 * 1. 每个节点计算多个虚拟节点（均匀分布）
 * 2. 服务名计算 hash，顺时针找到最近的节点
 * 3. 节点增减时，只影响相邻节点的数据
 *
 * 对照：com.alibaba.nacos.naming.core.v2.distro.DistroMapper
 */
public class ConsistentHashRing {

    /**
     * 虚拟节点数（每个真实节点对应 100 个虚拟节点）
     */
    private static final int VIRTUAL_NODES_PER_REAL = 100;

    /**
     * 哈希环：hash -> Member
     */
    private final TreeMap<Long, Member> ring;

    /**
     * 真实节点集合
     */
    private final Set<Member> realMembers;

    public ConsistentHashRing() {
        this.ring = new TreeMap<>();
        this.realMembers = new HashSet<>();
    }

    /**
     * 添加节点
     */
    public void addMember(Member member) {
        if (realMembers.contains(member)) {
            return;
        }

        realMembers.add(member);

        // 添加虚拟节点
        for (int i = 0; i < VIRTUAL_NODES_PER_REAL; i++) {
            String virtualKey = member.getAddress() + "#" + i;
            long hash = hash(virtualKey);
            ring.put(hash, member);
        }

        System.out.println("[HashRing] Member added: " + member.getAddress()
            + " with " + VIRTUAL_NODES_PER_REAL + " virtual nodes");
    }

    /**
     * 移除节点
     */
    public void removeMember(Member member) {
        if (!realMembers.contains(member)) {
            return;
        }

        realMembers.remove(member);

        // 移除虚拟节点
        for (int i = 0; i < VIRTUAL_NODES_PER_REAL; i++) {
            String virtualKey = member.getAddress() + "#" + i;
            long hash = hash(virtualKey);
            ring.remove(hash);
        }

        System.out.println("[HashRing] Member removed: " + member.getAddress());
    }

    /**
     * 根据 key 获取负责的节点
     */
    public Member getResponsibleMember(String key) {
        if (ring.isEmpty()) {
            return null;
        }

        long hash = hash(key);

        // 顺时针找到第一个节点
        Map.Entry<Long, Member> entry = ring.ceilingEntry(hash);
        if (entry == null) {
            // 如果超过最大 hash，回到第一个节点
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    /**
     * 获取所有真实节点
     */
    public Set<Member> getAllMembers() {
        return new HashSet<>(realMembers);
    }

    /**
     * 获取节点数量
     */
    public int getMemberCount() {
        return realMembers.size();
    }

    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return ring.isEmpty();
    }

    /**
     * 计算 hash 值（MD5 算法）
     */
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());

            // 取前 8 字节作为 long
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }

            // 确保非负
            return Math.abs(hash);
        } catch (NoSuchAlgorithmException e) {
            // 降级到简单 hash
            return Math.abs(key.hashCode());
        }
    }
}
