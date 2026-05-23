 **后端服务 (resms)**
- 基于 Spring Boot 3.5.14 的 Maven 多模块项目。
- 核心框架：Spring Boot 3.5.14
- 安全认证：Spring Security + JWT (JJWT 0.12.6)
- 持久层：MyBatis-Plus 3.5.14 + MySQL 8.4
- 缓存与通信：Redis + WebSocket (系统消息推送)
- 辅助工具：Lombok 1.18.46, Hutool 5.8.41, EasyExcel 4.0.3

 **推荐与助手服务 (ai-assistant)**
- 大语言模型：DeepSeek API
- 向量化模型：DashScope Embedding (text-embedding-v3)
- 向量数据库：PostgreSQL (pgvector 插件，HNSW 索引)

**管理后台前端 (resms-admin-ui)**
- 技术栈：Vue 3 + Vite 8 + TypeScript 6 + Element Plus + Tailwind CSS 4
- 特性：动态路由加载、基于 v-hasPermi 的按钮级权限控制、Echarts 报表统计。

**客户门户前端 (resms-portal-ui)**
- 技术栈：Vue 3 + Vite 7 + TypeScript 5.9 + Element Plus + Tailwind CSS 4
- 核心功能：房产浏览、预约看房、收藏与足迹、实时 AI 聊天咨询（WebSocket 直连）。

``` text
**项目目录结构**
├── resms/                     # Java 后端多模块工程
│   ├── resms-common/          # 通用工具类、AOP 日志、JWT 与安全组件
│   ├── resms-framework/       # 单体入口、安全、全局异常、MyBatis-Plus、WebSocket 配置
│   └── resms-modules/         # 业务模块聚合 POM
│       ├── resms-system/      # 用户、角色、菜单、部门及操作日志管理
│       ├── resms-house/       # 楼盘、房源、图片管理与状态跟踪
│       ├── resms-trade/       # 客户、订单、预约与历史足迹
│       ├── resms-finance/     # 佣金、支付、财务报表
│       ├── resms-integration/ # 文件上传、内聊、系统通知与 WebSocket 事件监听
│       ├── resms-portal/      # 客户门户 API 适配层
│       └── resms-AiService/   #  推荐与画像服务接口
├── ai-assistant/              #  推荐与助手服务 (Spring Boot + Spring AI)
├── resms-admin-ui/            # B 端管理后台 (Vue 3)
├── resms-portal-ui/           # C 端客户门户 (Vue 3)
└── resms.sql                  # 初始化 MySQL 数据库脚本
```

**环境准备**
运行本项目前，请确保本地已安装并配置以下中间件或服务：
- MySQL 8.4：导入根目录下的 resms.sql 初始化结构与基础数据。
- Redis：提供 Session 缓存、限流及会话状态管理（默认端口 6379）。
- PostgreSQL + pgvector：用于 ai-assistant 的向量检索与 Chat Memory 存储。
- API 密钥环境变量配置：
- DEEPSEEK_API_KEY：DeepSeek 大模型密钥。
- DASHSCOPE_API_KEY：阿里灵积嵌入模型密钥。
- JWT_SECRET：HS512 签名算法所用的 JWT 密钥。
