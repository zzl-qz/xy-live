<div align="center">

# XY Live

**一个基于 Spring Boot 3 + Dubbo 3 + Netty 的分布式直播电商后端**

直播 / 即时通讯 / 礼物打赏 / 红包雨 / 直播间商城 / 虚拟账户

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Dubbo](https://img.shields.io/badge/Dubbo-3.3.0-1f6feb.svg)](https://dubbo.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](./LICENSE)

[在线官网](https://zzl-qz.github.io/xy-live/) · [文档索引](./docs/README.md) · [贡献指南](./CONTRIBUTING.md)

</div>

---

## 项目简介

XY Live 是一套面向直播场景的后端系统，按业务域拆分为多个 Spring Boot 微服务，服务之间通过 **Dubbo RPC** 同步调用、通过 **RocketMQ** 异步解耦，统一由 **Nacos** 完成注册发现与配置下发，入口由 **Spring Cloud Gateway** 承担。

IM 部分没有依赖现成的推送服务，而是基于 **Netty** 自研了长连接网关（WebSocket / TCP 双协议），实现登录鉴权、心跳保活、消息编解码、在线路由、ACK 可靠投递与离线消息落 MQ。

项目最初的目的是把分布式系统里那些「纸上谈兵」的东西真正写一遍并跑通：分库分表、多级缓存、分布式锁、缓存一致性、消息最终一致性、限流、压测调优。因此代码中保留了较多显式的性能设计与注释，也保留了真实的压测报告。

> 项目处于持续迭代中，未做生产级打磨，请勿直接用于生产环境。已知限制见 [已知限制](#已知限制)。

## 功能特性

| 能力 | 说明 |
| --- | --- |
| 直播房间 | 开播 / 关播、房间分页、房间信息、普通直播与 PK 直播 |
| 即时通讯 | Netty 长连接网关，WebSocket + TCP，心跳、ACK、在线路由、离线消息 |
| 礼物打赏 | 礼物配置、送礼、礼物记录、PK 场景计分 |
| 红包雨 | 主播发红包、观众抢红包、异步结算 |
| 直播间商城 | 商品 / SKU、购物车、预下单、支付回调、库存扣减与回滚 |
| 虚拟账户 | 星币账户、交易流水，与支付服务联动 |
| 用户中心 | 手机号一键注册登录、用户标签、批量查询 |
| 基础组件 | 分布式 ID 发号器、短信验证码、XXL-Job 调度 |
| 前端 Demo | `front/` 下的 React + TS + Vite 演示端，覆盖登录、房间、聊天、送礼、商城 |

## 系统架构

```mermaid
flowchart TD
    Client[Web / App 客户端] --> GW[xy-live-gateway<br/>Spring Cloud Gateway :9191]
    Client -. WebSocket / TCP .-> IM[xy-live-im-core-server<br/>Netty 长连接网关]

    GW --> API[xy-live-api<br/>HTTP 聚合层]
    GW --> BAPI[xy-live-bank-api<br/>支付回调]

    API -->|Dubbo| User[user-provider]
    API -->|Dubbo| Living[living-provider]
    API -->|Dubbo| Gift[gift-provider]
    API -->|Dubbo| Bank[bank-provider]
    API -->|Dubbo| Msg[msg-provider]
    API -->|Dubbo| ImP[im-provider]

    IM -->|Dubbo| Router[im-router-provider]
    Router -->|Dubbo| IM

    Gift -. RocketMQ .-> Bank
    Gift -. RocketMQ .-> Msg
    IM -. RocketMQ .-> Msg
    User -. RocketMQ .-> User

    User --> IdGen[id-generate-provider]

    subgraph 基础设施
        Nacos[(Nacos<br/>注册 & 配置)]
        MySQL[(MySQL 8<br/>ShardingSphere 分表)]
        Redis[(Redis<br/>缓存 & 分布式锁)]
        RMQ[(RocketMQ<br/>异步消息)]
        XXL[XXL-Job<br/>定时任务)]
    end

    User --> MySQL
    Gift --> MySQL
    Bank --> MySQL
    IdGen --> MySQL
    Nacos --- GW & API & User & Living & Gift & Bank & Msg & IM
```

**分层约定**：每个业务域拆成 `-interface` 与 `-provider` 两个模块。`-interface` 只放 Dubbo 接口、DTO、枚举与常量，不依赖 Spring；`-provider` 放实现、DAO 与 Spring 配置。接入层（`xy-live-api`）只做参数校验、上下文透传与 RPC 编排，不直接访问数据库。

## 技术栈

| 分类 | 选型 | 版本 |
| --- | --- | --- |
| 语言 / 运行时 | Java | 17 |
| 基础框架 | Spring Boot | 3.0.4 |
| 微服务 | Spring Cloud Alibaba | 2022.0.0.0-RC1 |
| RPC | Apache Dubbo | 3.3.0 |
| 注册中心 / 配置中心 | Nacos | 2.x |
| 网关 | Spring Cloud Gateway + LoadBalancer | 随 Spring Cloud |
| ORM | MyBatis-Plus | 3.5.3.1 |
| 分库分表 | ShardingSphere-JDBC | 5.3.2 |
| 数据库 | MySQL | 8.0.x |
| 缓存 | Redis + Redisson | — |
| 本地缓存 | Caffeine | — |
| 消息队列 | RocketMQ | 4.9.6 |
| 长连接 | Netty | 4.1.18.Final |
| 任务调度 | XXL-Job | 2.4.0 |
| 短信 | 容联云短信 SDK | 1.0.4 |
| 前端 Demo | React + TypeScript + Vite | — |

## 模块说明

### 接入层

| 模块 | 职责 |
| --- | --- |
| `xy-live-gateway` | Spring Cloud Gateway 网关，监听 `9191`，路由规则由 Nacos 配置下发；负责限流、灰度、统一鉴权入口 |
| `xy-live-api` | HTTP 聚合层，对外提供直播、礼物、商城、用户、IM 配置等 REST 接口，内部通过 Dubbo 调用各 provider |
| `xy-live-bank-api` | 支付回调入口，接收微信支付异步通知并驱动订单结算 |

### 业务服务

| 模块 | 职责 |
| --- | --- |
| `xy-live-user-provider` / `-interface` | 用户信息、手机号注册登录、用户标签；热点数据三级缓存 + 分 100 表 |
| `xy-live-living-provider` / `-interface` | 直播间生命周期、房间列表、PK 连麦 |
| `xy-live-gift-provider` / `-interface` | 礼物配置与送礼、红包雨、SKU / 购物车 / 订单 / 库存 |
| `xy-live-bank-provider` / `-interface` | 虚拟币账户、交易流水、订单支付 |
| `xy-live-account-provider` / `-interface` | 账户 Token 签发与校验 |
| `xy-live-msg-provider` / `-interface` | 短信验证码、IM 业务消息消费与转发 |

### 即时通讯

| 模块 | 职责 |
| --- | --- |
| `xy-live-im-core-server` / `-interface` | Netty 长连接服务器，处理连接、鉴权、心跳、编解码与消息分发 |
| `xy-live-im-router-provider` / `-interface` | IM 消息路由，按用户定位到具体长连接节点 |
| `XY-live-im-provider` / `xy-live-im-interface` | IM Token 与在线状态查询 |

### 基础支撑

| 模块 | 职责 |
| --- | --- |
| `xy-live-id-generate-provider` / `-interface` | 基于号段的分布式 ID 生成器，Dubbo 端口 `9103` |
| `xy-live-common-interface` | 跨服务共享的 DTO、枚举、MQ Topic 常量、AES 与 Bean 转换工具 |

### 自研框架（`xy-live-framework/`）

| Starter | 能力 |
| --- | --- |
| `xy-live-framework-web-starter` | 全局异常处理、统一错误码、用户上下文、接口限流 |
| `xy-live-framework-datasource-starter` | ShardingSphere 数据源初始化与连接预热 |
| `xy-live-framework-redis-starter` | RedisTemplate 序列化配置、Redisson 客户端、统一 CacheKey 构建器 |
| `xy-live-framework-mq-starter` | RocketMQ 生产者 / 消费者自动装配与属性绑定 |
| `xy-live-framework-dubbo-starter` | Dubbo 调用白名单 |
| `xy-live-framework-xxljob-starter` | XXL-Job 执行器自动配置 |

## 目录结构

```text
xy-living-app-backend-provider/
├── xy-live-gateway/                 # 网关
├── xy-live-api/                     # HTTP 聚合层
├── xy-live-bank-api/                # 支付回调
├── xy-live-*-provider/              # 业务服务实现
├── xy-live-*-interface/             # 业务服务 Dubbo 接口与 DTO
├── xy-live-im-core-server/          # Netty IM 服务器
├── xy-live-framework/               # 自研 starter 集合
├── xy-live-common-interface/        # 公共契约
├── config/                          # 本地配置模板说明
├── docs/                            # 文档总索引
├── site/                            # 项目官网（部署到 GitHub Pages）
├── front/                           # React 演示前端
├── .env.example                     # 环境变量模板
└── pom.xml                          # 聚合 POM
```

## 快速开始

### 0. 获取代码

```bash
git clone https://github.com/zzl-qz/xy-live.git
cd xy-live
```

### 环境要求

| 组件 | 版本 / 端口 |
| --- | --- |
| JDK | 17+ |
| Maven | 3.8+ |
| Nacos | 2.x，`8848` |
| MySQL | 8.0，`3306` |
| Redis | 6+，`6379` |
| RocketMQ | 4.9.x，NameServer `9876` |
| XXL-Job | 2.4.0（可选，仅调度相关服务需要） |
| Node.js | 18+（仅前端 Demo 需要） |

### 1. 启动中间件

按上表启动 Nacos、MySQL、Redis、RocketMQ。XXL-Job 未启动时，仅影响依赖定时任务的模块（如购物车清理、库存同步）。

### 2. 初始化数据库

需要两个库：

| 库名 | 用途 |
| --- | --- |
| `xy_live_user` | 用户域。包含 `t_user_00` ~ `t_user_99`、`t_user_tag_00` ~ `t_user_tag_99`、`t_user_phone_00` ~ `t_user_phone_99` 共 300 张分表，以及 `t_user_index` |
| `xy_live_common` | 公共库，存放分布式 ID 号段表等 |

> 建表 SQL 脚本暂未随仓库提供，请按 `xy-live-user-provider/src/main/resources/xy-db-sharding.yaml` 中的 `actualDataNodes` 与各模块 `dao/po` 实体自行建表。后续会补充 `docs/sql/` 初始化脚本。

### 3. 准备配置

项目采用 **Nacos 配置中心** 管理运行时配置，本地 `bootstrap.yml` 只保留 Nacos 地址等引导信息。

1. 复制环境变量模板并填入本地真实值：

   ```bash
   cp .env.example .env
   ```

   在 IDE 运行配置或 Shell 中加载 `.env`（`NACOS_SERVER_ADDR`、`XY_DB_*`、`SPRING_DATA_REDIS_*`、`XY_RMQ_*` 等）。

2. 在 Nacos 中为每个服务创建配置文件：`${spring.application.name}.yml`，例如 `xy-live-user-provider.yml`、`xy-live-gateway.yaml`。运行时配置（端口、数据源、Dubbo、MQ 等）均在此维护。

3. 如需完全本地覆盖，可将 `config/application-local.example.yml` 复制为 `config/application-local.yml`，并以附加配置方式启动：

   ```bash
   --spring.config.additional-location=optional:file:./config/application-local.yml
   ```

   `config/application-local.yml` 与 `.env` 均已被 `.gitignore` 忽略，请勿提交真实凭据。

### 4. 构建

```bash
# 在根目录执行，需先安装 interface / framework 模块
mvn -T 1C clean install -DskipTests
```

### 5. 启动顺序

服务启动顺序建议遵循依赖方向：

```text
Nacos / MySQL / Redis / RocketMQ
        ↓
id-generate-provider → account-provider → user-provider
        ↓
living-provider → gift-provider → bank-provider → msg-provider
        ↓
im-core-server → im-router-provider → im-provider
        ↓
xy-live-api → xy-live-bank-api → xy-live-gateway
```

启动类一览：

| 服务 | 启动类 |
| --- | --- |
| 网关 | `org.xy.live.gateway.GatewayApplication` |
| HTTP 聚合层 | `org.zzl.xy.live.api.ApiWebApplication` |
| 支付回调 | `org.xy.live.bank.api.BankApiApplication` |
| 用户 | `org.xy.live.user.provider.UserProviderApplication` |
| 直播 | `org.xy.live.living.provider.LivingProviderApplication` |
| 礼物 | `org.xy.live.gitf.provider.GiftProviderApplication` |
| 账户 | `org.xy.live.account.provider.AccountProviderApplication` |
| 虚拟币 | `org.xy.live.bank.provider.BankProviderApplication` |
| 消息 | `org.xy.live.msg.provider.MsgProviderApplication` |
| IM 核心 | `org.xy.live.im.core.server.ImCoreServerApplication` |
| IM 路由 | `org.xy.live.im.router.provider.ImRouterProviderApplication` |
| IM Token | `org.xy.live.im.provider.ImProviderApplication` |
| ID 生成 | `org.xy.live.id.generate.provider.IdGenerateProviderApplication` |

启动完成后通过网关访问，默认 `http://localhost:9191`。网关路由前缀由 Nacos 配置决定，仓库未内置默认值。

### 6. 前端 Demo（可选）

```bash
cd front
npm install
cp .env.example .env
npm run dev
```

打开 `http://localhost:5173`。Vite 会把 `/api` 代理到 `VITE_BACKEND_ORIGIN`（默认 `http://localhost:9191`）。详见 [`front/README.md`](./front/README.md)。

## 文档索引

| 文档 | 内容 |
| --- | --- |
| [`docs/README.md`](./docs/README.md) | 文档总索引 |
| [`xy-live-user-provider/docs/user-provider.md`](./xy-live-user-provider/docs/user-provider.md) | 用户服务接口、缓存 Key、时序图、FAQ |
| [`xy-live-user-provider/docs/模块亮点设计文档.md`](./xy-live-user-provider/docs/模块亮点设计文档.md) | 用户服务高并发与并发安全设计 |
| [`xy-live-user-provider/docs/登录接口压测报告.md`](./xy-live-user-provider/docs/登录接口压测报告.md) | 登录接口压测数据 |
| [`xy-live-user-provider/docs/用户批量查询压测报告.md`](./xy-live-user-provider/docs/用户批量查询压测报告.md) | 批量查询压测数据 |
| [`xy-live-user-provider/docs/单用户查询接口压测报告.md`](./xy-live-user-provider/docs/单用户查询接口压测报告.md) | 单用户查询压测数据 |
| [`xy-live-im-core-server/docs/模块说明.md`](./xy-live-im-core-server/docs/模块说明.md) | Netty IM 服务器实现流程与设计点 |
| [`xy-live-msg-provider/docs/xy-live-msg-provider.md`](./xy-live-msg-provider/docs/xy-live-msg-provider.md) | 消息服务与验证码流程 |
| [`config/README.md`](./config/README.md) | 本地配置加载方式 |
| [`front/README.md`](./front/README.md) | 前端 Demo 使用说明 |
| [`site/README.md`](./site/README.md) | 官网维护与 GitHub Pages 部署说明 |

## 设计要点

- **用户域三级缓存**：布隆过滤器 → Redis → MySQL，缓存 TTL 带随机抖动；回源加 Redisson 分布式锁，防穿透与击穿。
- **缓存一致性**：写路径采用「更新 DB → 删除缓存 → 延迟消息二次删除」的延迟双删策略。
- **分库分表**：ShardingSphere-JDBC `INLINE` 算法，按 `user_id % 100` 水平分 100 表。
- **IM 可靠投递**：自定义 `ImMsg` 协议（magic + len + code + body），握手鉴权、心跳保活、ACK 确认、离线消息转 MQ。
- **消息最终一致性**：送礼、支付、库存回滚等跨域动作通过 RocketMQ 事件驱动。
- **ID 生成**：号段模式，DB 批量分配 + 本地双 Buffer 预取，避免每次取号打库。

## 已知限制

- 建表 SQL 未随仓库提供，需要按实体与分表配置自行初始化。
- 运行时配置依赖 Nacos，仓库内不包含 `bootstrap.yml` 之外的服务配置。
- 短信验证码在测试环境下固定为 `123456`，接入真实短信通道前请替换。
- 未提供推流 / 播放地址服务，前端播放器需自行配置可访问的播放源。
- 暂未提供完整的单元测试与 CI 流水线。

## 贡献

欢迎提交 Issue 与 Pull Request。开始之前请阅读 [CONTRIBUTING.md](./CONTRIBUTING.md)。

## License

本项目基于 [MIT License](./LICENSE) 开源。
