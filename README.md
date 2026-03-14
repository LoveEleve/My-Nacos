# My Nacos - 手写服务注册中心

> 基于「问题驱动的逆向工程学习」方法手写 Nacos 核心功能

## 项目结构

```
my_nacos/
├── my-nacos-server/     # 服务端模块
│   └── src/main/java/com/mynacos/server/
│       ├── ServiceManager.java          # 服务管理器（三层Map结构）
│       ├── Service.java                 # 服务定义
│       ├── Instance.java                # 实例定义
│       ├── HealthCheckProcessor.java    # 健康检查（心跳）
│       └── NacosServer.java             # HTTP服务端
├── my-nacos-client/     # 客户端模块
│   └── src/main/java/com/mynacos/client/
│       └── NacosClient.java             # 客户端实现
└── docs/
    └── 手写Nacos问题驱动指南.md          # 完整学习文档
```

## 快速开始

### 1. 启动服务端

```java
NacosServer server = new NacosServer(8848);
server.start();
```

### 2. 客户端注册服务

```java
NacosClient client = new NacosClient("localhost:8848");
client.registerInstance("order-service", "192.168.1.100", 8080);
```

### 3. 客户端发现服务

```java
String instances = client.getInstances("order-service");
System.out.println(instances);
```

## 阶段1：单机 MVP（已完成 ✅）

- [x] 服务端存储结构（ServiceManager - 三层Map）
- [x] 读多写少优化（CopyOnWriteArrayList）
- [x] 健康检查（心跳机制）
- [x] HTTP服务端（注册/查询/心跳接口）
- [x] 客户端实现（自动心跳）

## 阶段2：生产特性（待开发）

- [ ] 连接管理器（ConnectionManager）
- [ ] 服务订阅与推送
- [ ] 客户端本地缓存
- [ ] 故障转移

## 阶段3：集群一致性（待开发）

- [ ] Distro协议实现
- [ ] 一致性哈希
- [ ] 节点发现与加入

## 核心设计

| 问题 | 解决方案 | 核心代码 |
|------|----------|----------|
| 服务怎么存？ | 三层 Map | `ServiceManager.serviceMap` |
| 读多写少？ | CopyOnWrite | `Service.instances` |
| 健康检查？ | 心跳机制 | `HealthCheckProcessor` |
| 海量连接？ | 连接池 | 待实现 |

## 学习文档

详见 [手写Nacos问题驱动指南.md](docs/手写Nacos问题驱动指南.md)

## 许可证

MIT
