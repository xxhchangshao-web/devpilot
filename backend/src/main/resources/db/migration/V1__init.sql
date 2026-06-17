-- ============================================
-- DevPilot 用户认证模块初始化（PostgreSQL）
-- ============================================

CREATE TABLE t_user (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'DEVELOPER',
    enabled     SMALLINT     NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted  SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_user_enabled ON t_user (enabled);
CREATE INDEX idx_user_is_deleted ON t_user (is_deleted);

-- 系统演示数据创建者：禁用账号，不能登录。
INSERT INTO t_user (id, username, password, role, enabled) VALUES
(1, 'system', '$2b$10$hgpOaoKvLpQxmrR1rGe2C.sSWCfb1qA/x/649JGbScdVVqYxlTIdG', 'ADMIN', 0);

-- 公开只读演示账号，密码：DevPilotDemo2026!
INSERT INTO t_user (id, username, password, role, enabled) VALUES
(2, 'demo', '$2b$10$MjsNT4b5y5G2s8n7m1XWjO/BNEka3Ix9HSJdY4PEPi32EweyL8jRG', 'VIEWER', 1);

SELECT setval('t_user_id_seq', (SELECT MAX(id) FROM t_user));
