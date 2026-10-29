# 本地配置说明

本目录存放**配置模板**，真实的本地配置不会进入版本库。

## 使用方式

1. 复制模板：

   ```bash
   cp config/application-local.example.yml config/application-local.yml
   ```

   Windows PowerShell：

   ```powershell
   Copy-Item config/application-local.example.yml config/application-local.yml
   ```

2. 填入本地真实的数据库、Redis、RocketMQ、Nacos 凭据。

3. 启动服务时附加该配置文件：

   ```text
   --spring.config.additional-location=optional:file:./config/application-local.yml
   ```

   在 IDEA 中可填入对应服务的 Run Configuration → *VM options* 或
   *Program arguments*。

## 与 Nacos 的关系

服务启动时会从 Nacos 拉取 `${spring.application.name}.yml` 作为主配置，
本地的 `application-local.yml` 仅作为运行时覆盖（例如临时把数据源指向本机）。

`bootstrap.yml` 中的 Nacos 地址与命名空间支持环境变量注入：

| 环境变量 | 说明 |
| --- | --- |
| `NACOS_SERVER_ADDR` | Nacos 地址，默认 `localhost:8848` |
| `NACOS_NAMESPACE` | Nacos 命名空间，默认空（public） |

## 环境变量

RocketMQ、ShardingSphere 数据源与 ID 生成器数据源同时支持环境变量配置，
完整清单见根目录 [`.env.example`](../.env.example)。

```bash
cp .env.example .env
```

在 Shell、IDE 运行配置或容器编排中加载 `.env` 后再启动服务。

## 安全约定

以下文件已被 `.gitignore` 忽略，**请勿提交**：

- `config/application-local.yml` 及任意 `*.local.yml`
- 根目录 `.env` 及任意 `.env.*`（`.env.example` 除外）
- `**/src/main/resources/application-local.*`
- `**/src/main/resources/secret/`

提交前请检查 `git diff`，确认没有密码、AccessKey、SecretKey、私钥证书或真实域名。
