# My Nacos - 手写服务注册中心

> 对照 Nacos 2.x 源码手写实现，问题驱动学习方法

## 项目目标

实现与 Nacos 2.x 架构一致的核心功能

## 核心架构

```
Service（不可变对象）
    ├── ServiceManager（单例）
    │   └── singletonRepository: Map<Service, Service>
    │
    Client（客户端抽象）
        ├── IpPortBasedClient
        │   └── serviceInstances: Map<Service, InstancePublishInfo>
        │
        └── ClientManager
            └── EphemeralIpPortClientManager
                └── clients: Map<clientId, Client>

索引与推送
    ├── ClientServiceIndexesManager（双向索引）
    │   ├── subscriberIndexes: Map<Service, Set<clientId>>
    │   └── subscriberClientIndexes: Map<clientId, Set<Service>>
    │
    ├── PushDelayTaskExecuteEngine（推送引擎）
    │   ├── 延迟500ms
    │   └── 任务合并
    │
    └── NamingPushService（推送实现）
```

## 快速开始

```bash
cd my-nacos
mvn compile -q

# 单元测试
java -cp target/classes com.mynacos.NacosUnitTest

# 推送测试
java -cp target/classes com.mynacos.NacosPushTest
```

## 已对齐源码

### 核心架构（阶段1）

| 组件 | 路径 | 状态 |
|------|------|------|
| Service | `naming/core/v2/pojo/Service.java` | ✅ |
| ServiceManager | `naming/core/v2/ServiceManager.java` | ✅ |
| Client | `naming/core/v2/client/Client.java` | ✅ |
| IpPortBasedClient | `...client/impl/IpPortBasedClient.java` | ✅ |
| ClientManager | `...client/manager/ClientManager.java` | ✅ |
| EphemeralIpPortClientManager | `...manager/impl/EphemeralIpPortClientManager.java` | ✅ |

### HTTP 接口（阶段2进行中）

| 组件 | 路径 | 状态 |
|------|------|------|
| InstanceController | `naming/remote/InstanceController.java` | ✅ |
| NacosHttpServer | `naming/remote/NacosHttpServer.java` | ✅ |

### 订阅与推送（阶段2进行中）

| 组件 | 路径 | 状态 |
|------|------|------|
| ClientServiceIndexesManager | `naming/core/v2/index/ClientServiceIndexesManager.java` | ✅ |
| PushDelayTask | `naming/push/PushDelayTask.java` | ✅ |
| PushDelayTaskExecuteEngine | `naming/push/PushDelayTaskExecuteEngine.java` | ✅ |
| NamingPushService | `naming/push/NamingPushService.java` | ✅ |
| ServiceStorage | `naming/core/v2/index/ServiceStorage.java` | ✅ |

### 客户端缓存（阶段2进行中）

| 组件 | 路径 | 状态 |
|------|------|------|
| ServiceInfo | `naming/core/v2/pojo/ServiceInfo.java` | ✅ |
| ServiceInfoHolder | `naming/core/v2/ServiceInfoHolder.java` | ✅ |
| NacosClient | `naming/remote/NacosClient.java` | ✅ |

### 集群一致性（阶段3完成）

| 组件 | 路径 | 状态 |
|------|------|------|
| Member | `naming/core/v2/pojo/Member.java` | ✅ |
| ServerMemberManager | `naming/core/v2/ServerMemberManager.java` | ✅ |
| ConsistentHashRing | `naming/core/v2/ConsistentHashRing.java` | ✅ |
| DistroProtocol | `naming/consistency/distro/DistroProtocol.java` | ✅ |

## HTTP API

```
POST   /nacos/v1/ns/instance              - 注册实例
DELETE /nacos/v1/ns/instance              - 注销实例
GET    /nacos/v1/ns/instance/list         - 查询实例列表
PUT    /nacos/v1/ns/instance/beat         - 心跳
POST   /nacos/v1/ns/instance/subscribe    - 订阅服务（新增）
DELETE /nacos/v1/ns/instance/subscribe    - 取消订阅（新增）
```

## 测试

### 单元测试

```bash
java -cp target/classes com.mynacos.NacosUnitTest
```

验证：
- 注册/查询/注销实例
- Service 单例模式
- Client 管理

### 推送测试

```bash
java -cp target/classes com.mynacos.NacosPushTest
```

验证：
- 订阅服务
- 注册实例触发推送
- 延迟合并任务
- 注销实例触发推送

## 学习文档

[手写Nacos问题驱动指南.md](docs/手写Nacos问题驱动指南.md)

## 许可证

MIT
