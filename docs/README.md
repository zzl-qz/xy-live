# 文档索引

本目录汇总面向使用者的通用文档。各业务模块的详细设计文档放在模块自己的 `docs/` 目录下，便于与代码一起演进。

## 全局文档

| 文档 | 说明 |
| --- | --- |
| [项目 README](../README.md) | 项目简介、架构、技术栈、模块说明、快速开始 |
| [贡献指南](../CONTRIBUTING.md) | 开发环境、分支与提交规范、PR 流程 |
| [变更日志](../CHANGELOG.md) | 版本变更记录 |
| [环境变量模板](../.env.example) | 启动服务所需的环境变量 |
| [本地配置说明](../config/README.md) | 本地配置文件的加载方式与安全约定 |
| [前端说明](../front/README.md) | React 演示端的启动与能力说明 |
| [官网说明](../site/README.md) | GitHub Pages 站点的结构、本地预览与部署方式 |

## 模块文档

### 用户服务

| 文档 | 说明 |
| --- | --- |
| [user-provider.md](../xy-live-user-provider/docs/user-provider.md) | 接口清单、核心流程时序图、缓存 Key 规范、接入示例、FAQ |
| [模块亮点设计文档.md](../xy-live-user-provider/docs/模块亮点设计文档.md) | 三级缓存、分库分表、分布式锁、延迟双删等设计说明 |
| [登录接口压测报告.md](../xy-live-user-provider/docs/登录接口压测报告.md) | 登录接口在不同并发下的压测结果 |
| [单用户查询接口压测报告.md](../xy-live-user-provider/docs/单用户查询接口压测报告.md) | 单用户查询压测结果 |
| [用户批量查询压测报告.md](../xy-live-user-provider/docs/用户批量查询压测报告.md) | 批量查询压测结果与优化建议 |

### 即时通讯

| 文档 | 说明 |
| --- | --- |
| [模块说明.md](../xy-live-im-core-server/docs/模块说明.md) | Netty IM 服务器的连接流程、消息流程、设计模式与待优化点 |

### 消息服务

| 文档 | 说明 |
| --- | --- |
| [xy-live-msg-provider.md](../xy-live-msg-provider/docs/xy-live-msg-provider.md) | 短信验证码、IM 消息消费与转发、线程池配置 |

## 阅读建议

- **想跑起来**：先看根 [README](../README.md) 的「快速开始」，再按需查看 [环境变量模板](../.env.example)。
- **想改用户域**：先读 [user-provider.md](../xy-live-user-provider/docs/user-provider.md) 的接口与缓存 Key 规范，再看设计亮点文档。
- **想改 IM**：先读 [模块说明.md](../xy-live-im-core-server/docs/模块说明.md) 的连接与消息流程。
- **想提 PR**：先读 [贡献指南](../CONTRIBUTING.md)。

## 待补充文档

以下文档尚在规划中，欢迎认领：

- 数据库表结构与初始化脚本说明
- Nacos 配置项清单（各服务 `*.yml` 模板）
- 本地一键启动（Docker Compose）说明
- HTTP 接口文档（OpenAPI / Swagger）
- IM 消息协议格式规范
- 部署与运维手册
