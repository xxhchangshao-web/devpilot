# DevPilot V1.0 最终交付报告

**生成日期**: 2026-06-17
**项目路径**: `D:\dev\projects\personal\devpilot`

---

## 一、项目概述

DevPilot 是一个面向 Java 后端开发者的**工程问题排查助手**，帮助团队沉淀排查经验、快速检索复用。

| 维度 | 内容 |
|------|------|
| 产品定位 | 工程问题排查知识库 |
| 目标用户 | Java 后端开发者 / 测试人员 / 运维 |
| 技术栈 | Java 17 + Spring Boot 3 + MyBatis-Plus / Vue 3 + Element Plus |
| 数据库 | PostgreSQL (生产) / H2 (开发) |
| 部署方式 | Docker Compose 一键启动 |

---

## 二、交付物清单

### 2.1 需求与设计文档

| 文档 | 路径 |
|------|------|
| 全局需求 PRD | `DevPilot\V1.0 基础版\全局需求\requirements.md` |
| 模块清单 | `DevPilot\V1.0 基础版\全局需求\modules.yaml` |
| Design Token | `DevPilot\V1.0 基础版\全局需求\design-token.json` |
| 用户登录 PRD | `DevPilot\V1.0 基础版\用户登录\用户登录.md` |
| 问题笔记管理 PRD | `DevPilot\V1.0 基础版\问题笔记管理\问题笔记管理.md` |
| 标签分类 PRD | `DevPilot\V1.0 基础版\标签分类\标签分类.md` |
| Docker 部署 PRD | `DevPilot\V1.0 基础版\Docker 部署\Docker 部署.md` |
| 低保真原型 | `DevPilot\V1.0 基础版\{模块名}\低保真\*.html` |

### 2.2 SDD 编码文档

| 文档 | 路径 |
|------|------|
| 通用编码规范 | `.aspirecode\sdd\rules.md` |
| 后端专属规范 | `.aspirecode\sdd\backend-rules.md` |
| 前端专属规范 | `.aspirecode\sdd\frontend-rules.md` |
| 任务清单 | `.aspirecode\sdd\devpilot_v1.0.0\task.yaml` |
| 交接文件 | `.aspirecode\sdd\devpilot_v1.0.0\coding-handoff.md` |
| 各模块 SDD | `.aspirecode\sdd\devpilot_v1.0.0\{模块名}\` |

### 2.3 后端代码

```
backend/
├── pom.xml                              ← Maven 项目文件
├── src/main/java/com/devpilot/
│   ├── DevPilotApplication.java
│   ├── common/
│   │   ├── ApiResponse.java             ← 统一响应体
│   │   ├── BusinessException.java       ← 业务异常
│   │   └── GlobalExceptionHandler.java  ← 全局异常处理
│   ├── config/
│   │   └── SecurityConfig.java          ← Spring Security + JWT
│   ├── controller/
│   │   ├── AuthController.java          ← 登录/退出
│   │   ├── IssueNoteController.java     ← 笔记 CRUD
│   │   └── TagController.java           ← 标签 CRUD
│   ├── dto/                             ← 20+ 数据传输对象
│   ├── entity/
│   │   ├── User.java                    ← t_user
│   │   ├── IssueNote.java               ← t_issue_note
│   │   ├── Tag.java                     ← t_tag
│   │   └── NoteTag.java                 ← t_note_tag
│   ├── mapper/
│   │   ├── UserMapper.java
│   │   ├── IssueNoteMapper.java
│   │   ├── IssueNoteMapper.xml
│   │   ├── TagMapper.java
│   │   ├── NoteTagMapper.java
│   │   └── NoteTagMapper.xml
│   ├── security/
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── SecurityUser.java
│   └── service/
│       ├── AuthService.java
│       ├── IssueNoteService.java
│       ├── TagService.java
│       └── UserDetailsServiceImpl.java
├── src/main/resources/
│   ├── application.yml                  ← H2 dev + PostgreSQL prod
│   └── db/migration/
│       ├── V1__init.sql                 ← t_user + seed admin/dev
│       ├── V2__issue_note.sql           ← t_issue_note + t_note_tag
│       └── V3__create_tag_tables.sql    ← t_tag
└── src/test/                            ← 8 个测试类 99 用例
```

### 2.4 前端代码

```
frontend/
├── package.json
├── vite.config.ts                       ← Vite + /api 代理
├── index.html
├── nginx.conf                           ← Docker 反向代理
├── Dockerfile
└── src/
    ├── main.ts
    ├── App.vue
    ├── api/
    │   ├── request.ts                   ← Axios 封装
    │   ├── index.ts                     ← 登录 API
    │   ├── notes.ts                     ← 笔记 API
    │   └── tags.ts                      ← 标签 API
    ├── router/
    │   └── index.ts                     ← 路由守卫
    ├── store/
    │   └── user.ts                      ← Pinia 状态
    ├── utils/
    │   └── format.ts                    ← 日期格式化
    ├── views/
    │   ├── Login.vue
    │   ├── notes/
    │   │   ├── NoteList.vue
    │   │   ├── NoteDetail.vue
    │   │   └── NoteFormDialog.vue
    │   └── tags/
    │       └── index.vue
    └── components/
        ├── AppLayout.vue
        └── TagSelector.vue
```

### 2.5 部署文件

```
├── docker-compose.yml
├── backend/Dockerfile
├── frontend/Dockerfile
└── frontend/nginx.conf
```

---

## 三、API 接口清单（13 个）

| 模块 | 方法 | 路径 | 鉴权 |
|------|------|------|------|
| 认证 | POST | `/api/auth/login` | 无需 |
| 认证 | POST | `/api/auth/logout` | JWT |
| 笔记 | POST | `/api/notes/list` | JWT |
| 笔记 | POST | `/api/notes/detail` | JWT |
| 笔记 | POST | `/api/notes/create` | DEVELOPER/ADMIN |
| 笔记 | POST | `/api/notes/update` | 创建者/ADMIN |
| 笔记 | POST | `/api/notes/delete` | 创建者/ADMIN |
| 笔记 | POST | `/api/notes/changeStatus` | 创建者/ADMIN |
| 标签 | POST | `/api/tags/list` | JWT |
| 标签 | POST | `/api/tags/all` | JWT |
| 标签 | POST | `/api/tags/create` | DEVELOPER/ADMIN |
| 标签 | POST | `/api/tags/update` | DEVELOPER/ADMIN |
| 标签 | POST | `/api/tags/delete` | DEVELOPER/ADMIN |

---

## 四、数据库表

| 表名 | 说明 | 迁移脚本 |
|------|------|------|
| `t_user` | 用户表（含 seed admin/dev） | V1 |
| `t_issue_note` | 问题笔记主表 | V2 |
| `t_note_tag` | 笔记-标签关联表 | V2 |
| `t_tag` | 标签表 | V3 |

---

## 五、测试报告

### 后端

| 测试类 | 用例数 | 结果 |
|------|:--:|:--:|
| AuthControllerTest | 9 | ✅ |
| IssueNoteControllerTest | 18 | ✅ |
| TagControllerTest | 12 | ✅ |
| JwtTokenProviderTest | 12 | ✅ |
| AuthServiceTest | 4 | ✅ |
| IssueNoteServiceTest | 25 | ✅ |
| TagServiceTest | 13 | ✅ |
| UserDetailsServiceImplTest | 6 | ✅ |
| **合计** | **99** | **全部通过** |

### 前端 E2E (Playwright)

| 场景 | 结果 |
|------|:--:|
| 登录页加载 | ✅ |
| admin 登录 → /notes | ✅ |
| 笔记列表（筛选+表格+分页） | ✅ |
| 新建笔记弹窗 + 提交 | ✅ |
| 笔记详情页（编辑/删除/改状态） | ✅ |
| 标签管理页（表格+日期） | ✅ |
| 侧栏导航切换 | ✅ |
| 控制台错误 | 0 |

---

## 六、启动方式

### 方式一：本地开发

```bash
# 后端 (需 JDK 17)
cd backend
mvn spring-boot:run -Dmaven.test.skip=true

# 前端 (需 Node.js 20)
cd frontend
npm install && npm run dev

# 访问 http://localhost:5173
```

### 方式二：Docker 部署

```bash
cd D:\dev\projects\personal\devpilot
docker compose up -d

# 访问 http://localhost:80
```

### 公开演示账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 只读访客 | `demo` | `DevPilotDemo2026!` |

管理员账号由服务器 `.env` 中的 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 初始化，仅维护者使用，不写入公开文档。

---

## 七、技术亮点

| 特性 | 实现 |
|------|------|
| 认证鉴权 | Spring Security + JWT (HS256) |
| 密码加密 | BCrypt |
| 数据库迁移 | Flyway (H2 内存开发 / PostgreSQL 生产) |
| 逻辑删除 | MyBatis-Plus @TableLogic |
| 状态流转 | 状态机校验 (OPEN→IN_PROGRESS→RESOLVED→ARCHIVED) |
| 权限控制 | 角色 + 创建者所有权 (ROLE_ADMIN/DEVELOPER/VIEWER) |
| 统一响应 | ApiResponse<T> {code, message, data} |
| 全局异常 | GlobalExceptionHandler 分层处理 |
| 前端路由守卫 | beforeEach 拦截未登录 / 重定向已登录 |
| 日期格式化 | 兼容纳秒精度 ISO (H2 LocalDateTime) |

---

## 八、已知说明

| 项 | 说明 |
|------|------|
| JDK 要求 | JDK 17，系统默认 JDK 8 需设置 JAVA_HOME |
| 开发数据库 | H2 内存库 (MODE=MySQL)，Flyway 自动建表 |
| 生产数据库 | PostgreSQL 16 |
| curl 中文 | Windows Git Bash curl 中文编码问题，浏览器和 Python 正常 |
| 前端构建 | `vite build` 通过，`vue-tsc -b` 因预存测试 type 错误阻塞 |

---

## 九、公开部署加固

| 项 | 说明 |
|------|------|
| GitHub 发布 | `.gitignore` 已排除 IDE、agent、coverage、缓存和本地敏感目录 |
| 生产配置 | Docker Compose 仅开放前端 80，数据库和后端不直接暴露公网 |
| 账号安全 | `demo` 为只读演示账号，管理员密码只在服务器 `.env` 配置 |
| 数据库兼容 | Flyway 脚本已调整为 PostgreSQL 语法 |
| 演示数据 | 内置问题笔记和标签样例，归属禁用的 `system` 用户 |
