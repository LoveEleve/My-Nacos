# My Nacos - 手写服务注册中心

> 对照 Nacos 2.x 源码手写实现，基于「问题驱动的逆向工程学习」方法

## 项目目标

**v1 分支目标**：实现与 Nacos 2.x 架构一致的核心功能

- ✅ 真实架构：Service + Client 模型（不是玩具三层Map）
- ✅ 单例模式：ServiceManager 保证 Service 单例
- ✅ 版本控制：Service.revision 用于 Distro 同步
- ✅ 客户端抽象：Client 接口 + IpPortBasedClient 实现
- 🔄 连接管理：基于 gRPC 的长连接（待实现）
- 🔄 服务注册：HTTP/gRPC 接口（待实现）
- 🔄 集群同步：Distro 协议（待实现）

## 与真实 Nacos 2.x 的对比

| 模块 | 我们的实现 | Nacos 2.x 源码 | 状态 |
|------|-----------|----------------|------|
| Service | `com.mynacos.naming.core.v2.pojo.Service` | `com.alibaba.nacos.naming.core.v2.pojo.Service` | ✅ 已对齐 |
| ServiceManager | `com.mynacos.naming.core.v2.ServiceManager` | `com.alibaba.nacos.naming.core.v2.ServiceManager` | ✅ 已对齐 |
| Client | `com.mynacos.naming.core.v2.client.Client` | `com.alibaba.nacos.naming.core.v2.client.Client` | ✅ 已对齐 |
| IpPortBasedClient | `com.mynacos.naming.core.v2.client.impl.IpPortBasedClient` | `com.alibaba.nacos.naming.core.v2.client.impl.IpPortBasedClient` | ✅ 已对齐 |
| ClientManager | `com.mynacos.naming.core.v2.client.manager.ClientManager` | `com.alibaba.nacos.naming.core.v2.client.manager.ClientManager` | ✅ 已对齐 |
| EphemeralIpPortClientManager | `...impl.EphemeralIpPortClientManager` | `...impl.EphemeralIpPortClientManager` | ✅ 已对齐 |
| ConnectionBasedClientManager | 占位待实现 | `...impl.ConnectionBasedClientManager` | 🔄 待实现 |
| gRPC 服务 | 占位待实现 | `com.alibaba.nacos.naming.remote.rpc` | 🔄 待实现 |
| Distro 协议 | 占位待实现 | `com.alibaba.nacos.naming.consistency.ephemeral.distro` | 🔄 待实现 |

## 快速验证

```bash
cd my-nacos
mvn compile -q
java -cp target/classes com.mynacos.NacosDemo
```

预期输出：
```
=== My Nacos v2 Core Demo ===
...
Service 单例验证: true
从 ServiceManager 获取的是同一对象: true
...
Demo completed!
```

## 核心设计

### Service 单例模式

```java
// 通过 equals/hashCode 保证单例
Service s1 = Service.newService("public", "DEFAULT_GROUP", "order");
Service s2 = Service.newService("public", "DEFAULT_GROUP", "order");
assert s1.equals(s2);  // true
assert ServiceManager.getInstance().getSingleton(s1) 
    == ServiceManager.getInstance().getSingleton(s2);  // true
```

### Client 发布实例

```java
// Client 存储自己的实例
Client client = IpPortBasedClient.createEphemeralClient("192.168.1.100", 8080);
client.addServiceInstance(service, instanceInfo);
```

## 学习文档

详见 [手写Nacos问题驱动指南.md](docs/手写Nacos问题驱动指南.md)

## 许可证

MIT
