## 变更内容

<!-- 简要说明这个 PR 做了什么，以及为什么需要它 -->

## 关联 Issue

<!-- 例如 Closes #42 -->

## 变更类型

- [ ] 新功能（feat）
- [ ] 缺陷修复（fix）
- [ ] 重构（refactor）
- [ ] 性能优化（perf）
- [ ] 文档（docs）
- [ ] 构建 / 依赖 / 配置（chore）

## 影响范围

<!-- 勾选涉及的模块 -->

- [ ] `xy-live-gateway` / `xy-live-api` / `xy-live-bank-api`
- [ ] 用户域（user）
- [ ] 直播域（living）
- [ ] 礼物与商城（gift）
- [ ] 虚拟币与支付（bank / account）
- [ ] 消息（msg）
- [ ] IM（im-core-server / im-router / im-provider）
- [ ] 基础组件（id-generate）
- [ ] `xy-live-framework/*` starter
- [ ] `xy-live-common-interface`
- [ ] 前端 `front/`
- [ ] 文档

## 兼容性

- [ ] 无破坏性变更
- [ ] 涉及 Dubbo 接口签名变更（已在下方说明兼容方案）
- [ ] 涉及数据库表结构变更（已提供变更 SQL）
- [ ] 涉及新增配置项（已更新 `.env.example` 与模块文档）

## 验证方式

<!-- 说明如何验证该改动，例如启动哪些服务、调用了什么接口、观察到的结果 -->

1.
2.

## 自查清单

- [ ] 本地 `mvn -T 1C clean install -DskipTests` 通过
- [ ] 未提交任何真实凭据、密钥或用户隐私数据
- [ ] 新增模块已在根 `pom.xml` 的 `<modules>` 中注册
- [ ] 涉及接口 / 配置变更时已同步更新文档
- [ ] 提交信息符合 Conventional Commits 规范
