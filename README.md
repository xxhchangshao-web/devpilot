# DevPilot - 工程问题排查助手

DevPilot 是一个面向开发者的工程问题排查记录系统，用于沉淀故障现象、排查过程、解决方案和标签分类。项目覆盖前端管理台、后端认证鉴权、数据库迁移和 Docker Compose 部署，适合作为个人全栈工程能力展示。

## 在线演示

- 访问地址：`http://47.107.151.225`
- 演示账号：`demo`
- 演示密码：`DevPilotDemo2026!`
- 演示权限：只读浏览，不能新增、编辑、删除或变更状态

管理员账号仅用于维护演示环境，密码由服务器 `.env` 中的 `ADMIN_PASSWORD` 配置，不会提交到仓库。

## 核心功能

- JWT 登录认证与路由守卫
- 问题笔记分页、筛选、详情、创建、编辑、删除、状态流转
- 标签分类管理和笔记标签关联
- 角色权限控制：`ADMIN`、`DEVELOPER`、`VIEWER`
- Flyway 自动建表和演示数据初始化
- Docker Compose 一键部署前端、后端和 PostgreSQL

## 技术栈

后端：

- Java 17 + Spring Boot 3.3
- Spring Security + JWT
- MyBatis-Plus
- PostgreSQL 16
- Flyway
- Maven

前端：

- Vue 3 + TypeScript
- Vite
- Element Plus
- Pinia
- Vue Router
- Vitest

部署：

- Docker Compose
- Nginx 静态资源服务与 API 反向代理

## 项目结构

```text
devpilot/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/devpilot/
│   │   ├── config/             # Security、CORS、MyBatis、初始化账号
│   │   ├── controller/         # REST 控制器
│   │   ├── service/            # 业务逻辑
│   │   ├── mapper/             # MyBatis-Plus Mapper
│   │   ├── entity/             # 数据库实体
│   │   ├── dto/                # DTO / VO
│   │   └── security/           # JWT 认证
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/       # Flyway PostgreSQL 迁移脚本
├── frontend/                   # Vue 3 前端
│   └── src/
│       ├── api/
│       ├── components/
│       ├── router/
│       ├── store/
│       └── views/
├── DevPilot/                   # 需求与原型资料
├── docker-compose.yml
├── DEPLOY.md
└── README.md
```

## 本地开发

后端开发环境默认使用 H2 内存数据库：

```bash
cd backend
mvn spring-boot:run
```

前端开发：

```bash
cd frontend
npm install
npm run dev
```

## 生产部署

生产环境使用 PostgreSQL 和 Docker Compose。先复制环境变量模板并填写强密码：

```bash
cp .env.example .env
```

然后启动：

```bash
docker compose up -d --build
```

完整阿里云部署步骤见 [DEPLOY.md](DEPLOY.md)。

## 安全说明

- `.env`、IDE 配置、本地工具目录、测试覆盖率产物不会提交到 GitHub。
- 生产数据库和后端端口只在 Docker 内部网络访问，公网仅开放前端 `80`。
- `demo` 是只读演示账号，演示数据归属禁用的 `system` 用户，避免访客破坏数据。
- 管理员密码和 JWT 密钥只存在于服务器 `.env`。
