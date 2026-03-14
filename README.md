# My Nacos - 手写服务注册中心

> 对照 Nacos 2.x 源码手写实现，问题驱动学习方法

## 项目目标

实现与 Nacos 2.x 架构一致的核心功能

## 核心架构

```
Service（不可变对象）
    ├── ServiceManager（单例）
    │   ├── singletonRepository: Map<Service, Service>
    │   └── namespaceSingletonMaps: Map<namespace, Set<Service>>
    │
    Client（客户端抽象）
        ├── IpPortBasedClient（实现）
        │   └── serviceInstances: Map<Service, InstancePublishInfo>
        │
        └── ClientManager（管理器）
            └── EphemeralIpPortClientManager
                └── clients: Map<clientId, Client>
```

## 快速开始

```bash
cd my-nacos
mvn compile -q
java -cp target/classes com.mynacos.NacosDemo
```

## 已对齐源码

| 组件 | 路径 | 状态 |
|------|------|------|
| Service | `naming/core/v2/pojo/Service.java` | ✅ |
| ServiceManager | `naming/core/v2/ServiceManager.java` | ✅ |
| Client | `naming/core/v2/client/Client.java` | ✅ |
| IpPortBasedClient | `...client/impl/IpPortBasedClient.java` | ✅ |
| ClientManager | `...client/manager/ClientManager.java` | ✅ |
| EphemeralIpPortClientManager | `...manager/impl/EphemeralIpPortClientManager.java` | ✅ |

## 学习文档

[手写Nacos问题驱动指南.md](docs/手写Nacos问题驱动指南.md)

## 许可证

MIT
