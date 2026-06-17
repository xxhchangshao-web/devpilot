-- ============================================
-- 问题笔记表（PostgreSQL）
-- ============================================

CREATE TABLE t_issue_note (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    description   TEXT         NOT NULL,
    investigation TEXT,
    solution      TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    priority      VARCHAR(4)   NOT NULL DEFAULT 'P2',
    category      VARCHAR(20)  NOT NULL DEFAULT 'OTHER',
    creator_id    BIGINT       NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted    SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_issue_note_creator_id ON t_issue_note (creator_id);
CREATE INDEX idx_issue_note_status ON t_issue_note (status);
CREATE INDEX idx_issue_note_priority ON t_issue_note (priority);
CREATE INDEX idx_issue_note_category ON t_issue_note (category);
CREATE INDEX idx_issue_note_updated_at ON t_issue_note (updated_at);
CREATE INDEX idx_issue_note_created_at ON t_issue_note (created_at);
CREATE INDEX idx_issue_note_is_deleted ON t_issue_note (is_deleted);

-- 公开演示数据归属于禁用的 system 用户，避免 demo 账号具备创建者编辑权限。
INSERT INTO t_issue_note
    (id, title, description, investigation, solution, status, priority, category, creator_id, created_at, updated_at)
VALUES
    (1,
     '生产接口偶发超时排查：慢 SQL 与缺失索引定位',
     '订单查询接口在高峰期偶发 8 秒以上响应，影响客服查询效率。',
     '1. 通过网关日志确认慢请求集中在订单列表接口。\n2. 对比 APM 调用链，数据库查询耗时占比最高。\n3. 使用 EXPLAIN 分析发现 status + updated_at 条件未命中组合索引。\n4. 回放线上参数，确认分页深度增加后排序成本明显上升。',
     '增加业务查询组合索引，限制最大分页深度，并补充慢查询告警。上线后 P95 从 6.8 秒下降到 420 毫秒。',
     'RESOLVED', 'P1', 'PERFORMANCE', 1, CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '6 days'),
    (2,
     'JWT 登录失败问题：生产密钥未读取环境变量',
     '部署后登录接口返回 500，后端日志出现 JWT 签名相关异常。',
     '1. 检查容器环境变量确认 JWT_SECRET 已配置。\n2. 对照 application.yml 发现生产配置仍使用默认静态密钥。\n3. 本地复现后确认配置占位符缺失导致环境变量未生效。',
     '将 jwt.secret 改为读取 JWT_SECRET，并在 Docker Compose 中强制要求该变量存在。',
     'RESOLVED', 'P0', 'CONFIG', 1, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (3,
     'Docker 首次构建内存不足导致后端镜像失败',
     '2G 轻量服务器首次执行 docker compose build 时 Maven 构建阶段被系统终止。',
     '1. 观察 dmesg 发现进程因 OOM 被 kill。\n2. 对比 docker stats，Maven 下载依赖和打包阶段内存峰值较高。\n3. 服务器未配置 swap，系统没有缓冲空间。',
     '部署文档增加 2G swap 配置步骤，并建议首次构建期间不要同时执行其他高内存任务。',
     'IN_PROGRESS', 'P2', 'ENVIRONMENT', 1, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (4,
     '标签删除后笔记详情仍显示旧标签名',
     '删除标签后，部分笔记详情页仍显示已删除标签，容易误导排查记录分类。',
     '1. 检查删除接口确认已物理删除关联表记录。\n2. 查看详情接口发现标签名称来自临时的 tagId 占位逻辑。\n3. 对比标签模块上线后接口，确认需要统一从 t_tag 查询真实名称。',
     '待后续版本将笔记标签查询统一改为关联 t_tag，并过滤 is_deleted=0。',
     'OPEN', 'P2', 'BUSINESS_LOGIC', 1, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (5,
     '前端 401 处理优化：登录页错误提示不应强制跳转',
     '用户输错密码时页面偶发闪回登录页并重复弹出过期提示。',
     '1. 检查 Axios 响应拦截器，发现所有 401 都触发清 token 和跳转。\n2. 登录页认证失败也属于 401，但应由登录页展示“用户名或密码错误”。\n3. 修改拦截器后回归登录失败、token 过期两个场景。',
     '登录页 401 直接 reject，由 Login.vue 展示后端错误；非登录页 401 才清理登录态并跳转。',
     'RESOLVED', 'P3', 'EXCEPTION', 1, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP);

SELECT setval('t_issue_note_id_seq', (SELECT MAX(id) FROM t_issue_note));

-- ============================================
-- 笔记标签关联表（PostgreSQL）
-- ============================================

CREATE TABLE t_note_tag (
    id         BIGSERIAL PRIMARY KEY,
    note_id    BIGINT    NOT NULL,
    tag_id     BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_note_tag UNIQUE (note_id, tag_id)
);

CREATE INDEX idx_note_tag_note_id ON t_note_tag (note_id);
CREATE INDEX idx_note_tag_tag_id ON t_note_tag (tag_id);
