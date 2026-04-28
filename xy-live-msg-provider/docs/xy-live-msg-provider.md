# xy-live-msg-provider 模块说明文档

## 一、模块概述
`xy-live-msg-provider` 是直播系统中的消息服务核心模块，主要负责短信验证码管理、消息消费与转发等功能，基于 Spring Boot 构建，集成了分布式服务、消息队列、缓存等中间件，为整个直播系统提供可靠的消息传递支持。

## 二、核心功能
1. **短信验证码服务**
    - 生成并发送登录验证码（支持频率限制）
    - 校验验证码有效性
    - 记录短信发送记录到数据库

2. **消息消费与转发**
    - 消费 IM 服务发送的业务消息（如直播间聊天消息），处理后转发给目标用户
    - 消费银行服务发送的支付成功消息（待完善业务逻辑）

3. **分布式服务支持**
    - 通过 Dubbo 暴露 RPC 接口供其他服务调用
    - 基于 Nacos 实现服务注册与配置管理


## 三、技术栈与依赖
### 核心框架与中间件
- **基础框架**：Spring Boot、Spring Cloud
- **RPC 框架**：Dubbo 3.3.0
- **服务注册/配置**：Nacos
- **消息队列**：RocketMQ（通过自定义 starter 集成）
- **数据存储**：MySQL、Redis
- **ORM 框架**：MyBatis-Plus 3.5.3.1
- **数据库分表**：ShardingSphere 5.3.2

### 关键依赖
```xml
<!-- 核心依赖示例 -->
<dependencies>
    <!-- Dubbo 服务 -->
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-spring-boot-starter</artifactId>
        <version>3.3.0</version>
    </dependency>
    <!-- Nacos 服务发现与配置 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
    <!-- 数据库与 ORM -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>
    <!-- 短信 SDK -->
    <dependency>
        <groupId>com.cloopen</groupId>
        <artifactId>java-sms-sdk</artifactId>
        <version>1.0.4</version>
    </dependency>
    <!-- 自定义组件 -->
    <dependency>
        <groupId>org.zzl</groupId>
        <artifactId>xy-live-framework-redis-starter</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.zzl</groupId>
        <artifactId>xy-live-framework-mq-starter</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```


## 四、核心模块与流程
### 1. 短信服务流程
- **发送验证码**：`ISmsServiceImpl.sendLoginCode()`
    1. 校验手机号合法性及发送频率（基于 Redis 缓存限制 60 秒内重复发送）
    2. 生成验证码并缓存至 Redis（设置过期时间）
    3. 异步发送短信（通过线程池 `ThreadPoolManager` 执行）
    4. 发送成功后记录短信信息到数据库

- **校验验证码**：`ISmsServiceImpl.checkLoginCode()`
    1. 校验手机号和验证码格式
    2. 从 Redis 读取缓存的验证码进行比对
    3. 校验成功后删除缓存的验证码


### 2. 消息消费流程
- **IM 消息处理**：`ImMsgConsumer` + `SingleMessageHandlerImpl`
    1. 监听 RocketMQ 中 IM 服务发送的 `XY_LIVE_IM_BIZ_MSG_TOPIC` 主题消息
    2. 解析消息为 `ImMsgBody` 并交由 `SingleMessageHandlerImpl` 处理
    3. 针对直播间聊天消息，查询房间内在线用户并转发消息（通过 `ImRouterRpc` 路由）

- **银行消息处理**：`BankMsgConsumer`
    1. 监听 RocketMQ 中银行服务发送的 `XY_LIVE_BANK_PAY_SUCCESS_MSG_TOPIC` 主题消息
    2. 解析消息为 `BankMsgBody`（待完善业务逻辑，如发送支付成功短信）


## 五、配置说明
1. **服务配置**：`bootstrap.yml`
    - Nacos 服务地址与命名空间配置
    - 应用名称与配置文件导入规则

2. **线程池配置**：`ThreadPoolManager`
    - 核心线程数：2，最大线程数：8，队列容量：1000
    - 用于异步处理短信发送等非核心流程


## 六、模块亮点
1. **分布式架构适配**：集成 Dubbo + Nacos，支持服务注册、发现与远程调用，适配分布式部署场景
2. **异步化处理**：短信发送通过线程池异步执行，避免阻塞主流程，提升系统响应速度
3. **消息解耦设计**：基于 RocketMQ 消费各服务消息，降低服务间直接依赖，提高系统扩展性
4. **缓存策略优化**：使用 Redis 缓存验证码并设置过期时间，兼顾安全性与性能
5. **分层设计清晰**：严格区分数据访问层（DAO）、业务逻辑层（Service）、接口层（RPC），代码可读性高


## 七、待优化点
1. **验证码安全性**：当前验证码固定为 `123456`（测试用），需改为随机生成；缺少手机号格式校验
2. **消息消费可靠性**：银行消息消费逻辑未实现，需补充具体业务；消息处理异常时缺少重试与死信队列机制
3. **线程池配置**：线程池参数硬编码，建议改为可配置项；缺少拒绝策略，可能导致任务丢失
4. **数据库设计**：`SmsPO` 缺少字段注释，`sendTime` 和 `updateTime` 未实现自动填充
5. **依赖管理**：部分依赖版本未统一管理（如 Nacos 相关依赖），建议在父 POM 中集中定义版本号
6. **异常处理**：JSON 解析、RPC 调用等场景缺少完善的异常捕获与降级策略