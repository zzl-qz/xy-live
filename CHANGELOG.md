# 变更日志

本项目的所有重要变更都记录在此文件中。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### 新增

- 根目录 `README.md`：项目简介、架构图、技术栈、模块说明与快速开始。
- `CONTRIBUTING.md`：开发环境、分支与提交规范、代码规范、PR 流程。
- `LICENSE`：MIT 开源协议。
- `docs/README.md`：全局文档与模块文档索引。
- `site/`：项目官网单页（纯静态，零构建），含架构图、特性、快速开始、模块与技术栈介绍。
- `.github/workflows/deploy-pages.yml`：推送 `site/` 时自动部署到 GitHub Pages。
- GitHub Issue / Pull Request 模板。

### 变更

- `.gitignore`：取消对根目录 `docs/` 的忽略，允许提交开源文档。

## [1.0-SNAPSHOT] - 2026-10-29

### 新增

- 用户服务：手机号注册登录一体化、用户标签、批量查询，支持布隆过滤器 + Redis + MySQL 三级缓存、Redisson 分布式锁、延迟双删、按 `user_id % 100` 分表。
- 直播服务：直播间开播 / 关播、房间列表分页、PK 直播。
- 礼物服务：礼物配置与送礼、红包雨发放与领取、商品 SKU、购物车、预下单、订单与库存扣减回滚。
- 虚拟币服务：账户余额、交易流水、订单支付与支付回调。
- 账户服务：Token 签发与校验。
- 消息服务：短信验证码发送与校验、IM 业务消息消费转发。
- IM 体系：基于 Netty 的长连接服务器（WebSocket / TCP）、消息路由、Token 与在线状态服务。
- 分布式 ID 发号器：号段模式，Dubbo 端口 `9103`。
- 自研 starter：web、datasource、redis、mq、dubbo、xxljob。
- 网关：Spring Cloud Gateway，默认端口 `9191`。
- HTTP 聚合层 `xy-live-api` 与支付回调 `xy-live-bank-api`。
- 前端 Demo：React + TypeScript + Vite，覆盖登录、直播间、聊天、送礼与商城流程。
- 用户服务压测报告：登录、单用户查询、批量查询。

[Unreleased]: https://github.com/zzl-qz/xy-live/compare/v1.0-SNAPSHOT...HEAD
[1.0-SNAPSHOT]: https://github.com/zzl-qz/xy-live/releases/tag/v1.0-SNAPSHOT
