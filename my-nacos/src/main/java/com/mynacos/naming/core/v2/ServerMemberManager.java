/*
 * Copyright 2024 My-Nacos Authors.
 */

package com.mynacos.naming.core.v2;

import com.mynacos.naming.core.v2.pojo.Member;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ServerMemberManager - 集群成员管理器
 *
 * 问题：如何管理集群中的所有节点？
 *
 * 职责：
 * 1. 存储集群成员列表
 * 2. 管理当前节点信息
 * 3. 提供健康节点列表
 * 4. 节点变更通知
 *
 * 对照：com.alibaba.nacos.core.cluster.ServerMemberManager
 */
public class ServerMemberManager {

    /**
     * 单例实例
     */
    private static final ServerMemberManager INSTANCE = new ServerMemberManager();

    /**
     * 集群成员列表：address(ip:port) -> Member
     */
    private final ConcurrentHashMap<String, Member> memberMap;

    /**
     * 当前节点
     */
    private Member self;

    private ServerMemberManager() {
        this.memberMap = new ConcurrentHashMap<>();
    }

    public static ServerMemberManager getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化当前节点
     */
    public void initSelf(String ip, int port) {
        this.self = new Member(ip, port);
        memberMap.put(self.getAddress(), self);
        System.out.println("[MemberManager] Self initialized: " + self.getAddress());
    }

    /**
     * 更新成员
     */
    public void updateMember(Member member) {
        if (member == null || member.getAddress() == null) {
            return;
        }

        member.updateActiveTime();
        memberMap.put(member.getAddress(), member);
        System.out.println("[MemberManager] Member updated: " + member.getAddress());
    }

    /**
     * 移除成员
     */
    public void removeMember(String address) {
        Member removed = memberMap.remove(address);
        if (removed != null) {
            System.out.println("[MemberManager] Member removed: " + address);
        }
    }

    /**
     * 获取成员
     */
    public Member getMember(String address) {
        return memberMap.get(address);
    }

    /**
     * 获取所有成员
     */
    public Collection<Member> getAllMembers() {
        return memberMap.values();
    }

    /**
     * 获取所有健康成员（不包括自己）
     */
    public List<Member> getHealthyMembers() {
        return memberMap.values().stream()
                .filter(Member::isHealthy)
                .filter(m -> !m.equals(self))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有健康成员（包括自己）
     */
    public List<Member> getAllHealthyMembers() {
        return memberMap.values().stream()
                .filter(Member::isHealthy)
                .collect(Collectors.toList());
    }

    /**
     * 获取成员数量
     */
    public int getMemberCount() {
        return memberMap.size();
    }

    /**
     * 判断是否包含某成员
     */
    public boolean hasMember(String address) {
        return memberMap.containsKey(address);
    }

    /**
     * 获取当前节点
     */
    public Member getSelf() {
        return self;
    }

    /**
     * 判断是否是当前节点
     */
    public boolean isSelf(String address) {
        return self != null && self.getAddress().equals(address);
    }

    /**
     * 判断是否是当前节点
     */
    public boolean isSelf(Member member) {
        return self != null && self.equals(member);
    }
}
