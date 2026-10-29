# 贡献指南

感谢你愿意为 XY Live 贡献代码。本文档说明参与开发的方式与约定，请在提交 Issue 或 Pull Request 之前先阅读一遍。

## 目录

- [行为准则](#行为准则)
- [报告问题](#报告问题)
- [开发环境](#开发环境)
- [项目约定](#项目约定)
- [分支与提交](#分支与提交)
- [代码规范](#代码规范)
- [提交 Pull Request](#提交-pull-request)
- [文档贡献](#文档贡献)

## 行为准则

参与本项目即表示你同意：

- 尊重不同经验水平的参与者，对事不对人。
- 技术讨论聚焦方案与证据，避免人身攻击、歧视性言论与政治话题。
- 不提交他人的私密信息、真实用户数据或生产环境凭据。

违反上述约定的内容会被直接关闭或删除。

## 报告问题

提交 Issue 前请先搜索已有 Issue，避免重复。

**Bug 报告** 请尽量包含：

1. 复现步骤与环境（操作系统、JDK 版本、Maven 版本、中间件版本）。
2. 期望行为与实际行为。
3. 完整异常堆栈或关键日志（注意脱敏，删除密码、Token、手机号、真实 IP）。
4. 涉及的模块与类名。

**功能建议** 请说明使用场景与要解决的问题，而不是只描述实现方案。

**安全漏洞** 请勿公开提交 Issue，通过仓库维护者的 GitHub 主页邮箱私下联系。

## 开发环境

| 组件 | 版本 |
| --- | --- |
| JDK | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0 |
| Redis | 6+ |
| Nacos | 2.x |
| RocketMQ | 4.9.x |
| Node.js（仅 `front/`） | 18+ |

中间件与数据库的准备步骤见根目录 [`README.md`](./README.md#快速开始)。

### 本地配置

**不要提交任何真实凭据。** 本地配置通过以下两个入口注入，二者均已在 `.gitignore` 中忽略：

- 环境变量：复制 `.env.example` 为 `.env`，填入本地值。
- 附加配置文件：复制 `config/application-local.example.yml` 为 `config/application-local.yml`，启动时添加
  `--spring.config.additional-location=optional:file:./config/application-local.yml`。

提交前请自查：`git diff` 中不应出现密码、AccessKey、SecretKey、私有证书或真实域名。

### 构建

```bash
mvn -T 1C clean install -DskipTests
```

## 项目约定

### 模块拆分

新增业务域时遵循现有的 `-interface` / `-provider` 拆分：

- `xy-live-<domain>-interface`：Dubbo 接口、DTO、枚举、常量。**不得**依赖 Spring 或数据库相关组件。
- `xy-live-<domain>-provider`：服务实现、DAO、PO、MQ 消费者、定时任务。

接入层（`xy-live-api`）只做参数校验、上下文透传与 RPC 编排，**不要**在 Controller 中直接访问数据库。

### 新增模块

在业务模块中新增子模块后，需要同步在根 `pom.xml` 的 `<modules>` 中注册，否则不会被聚合构建。

### MQ

跨服务异步消息的 Topic 常量统一定义在 `xy-live-common-interface` 的 `topic` 包下，不要在业务模块内硬编码字符串。

### 缓存

缓存 Key 统一通过 `xy-live-framework-redis-starter` 中的 `*CacheKeyBuilder` 构建，不要在业务代码里拼接字符串。

## 分支与提交

### 分支命名

| 类型 | 格式 | 示例 |
| --- | --- | --- |
| 功能 | `feat/<简述>` | `feat/im-offline-msg` |
| 修复 | `fix/<简述>` | `fix/gift-stock-rollback` |
| 文档 | `docs/<简述>` | `docs/deploy-guide` |
| 重构 | `refactor/<简述>` | `refactor/user-cache` |

`master` 为稳定分支，请勿直接向 `master` 推送，一律通过 PR 合并。

### 提交信息

采用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/)：

```text
<type>(<scope>): <subject>

<body>

<footer>
```

| type | 含义 |
| --- | --- |
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `docs` | 文档 |
| `refactor` | 重构（不改变外部行为） |
| `perf` | 性能优化 |
| `test` | 测试 |
| `chore` | 构建、依赖、配置 |

示例：

```text
feat(gift): 支持红包雨按房间维度配置

新增 red_packet_config 房间级配置读取，兼容原有主播级配置。

Closes #42
```

一次提交只做一件事。避免「顺手格式化整个文件」这类淹没真实改动的提交。

## 代码规范

- 缩进 4 空格，UTF-8 编码，文件末尾保留换行。
- 类名 `UpperCamelCase`，方法与变量 `lowerCamelCase`，常量 `UPPER_SNAKE_CASE`。
- 优先使用构造器注入，避免字段注入。
- 事务方法注明超时与回滚条件；跨服务调用不要放在本地事务内。
- 新增公共异常使用 `xy-live-framework-web-starter` 的 `XyErrorException` + 错误码枚举，不要抛裸 `RuntimeException`。
- 日志使用 SLF4J 占位符写法，禁止字符串拼接；生产日志中禁止打印手机号、Token、密码明文。
- 不要在循环内发起 RPC 或数据库查询，批量场景请使用批量接口。

## 提交 Pull Request

1. Fork 仓库并从 `master` 切出功能分支。
2. 保持提交历史清晰，必要时先 rebase 整理。
3. 本地至少完成一次 `mvn -T 1C clean install -DskipTests` 通过。
4. 填写 PR 模板：变更内容、影响范围、验证方式、是否涉及数据库或配置变更。
5. 涉及数据库结构、MQ Topic、Dubbo 接口签名变更时，必须在 PR 描述中明确标注，并说明兼容方案。
6. 等待至少一位维护者 Review，讨论完成后再合并。

Review 过程中请通过追加提交回应意见，不要在 Review 期间 force push 覆盖历史，便于对照查看。

## 文档贡献

文档与代码同等重要。以下改动请同步更新文档：

| 改动 | 需要更新的文档 |
| --- | --- |
| 新增 / 删除模块 | 根 `README.md` 模块说明表、`docs/README.md` 索引 |
| 修改配置项 | 对应模块文档、`.env.example`、`config/application-local.example.yml` |
| 修改接口签名 | 对应模块 `docs/` 下的接口说明 |
| 新增中间件依赖 | 根 `README.md` 环境要求与技术栈 |

文档使用简体中文，采用 Markdown。流程图优先使用 Mermaid，便于在 GitHub 直接渲染。
