-- ============================================
-- 标签分类模块（PostgreSQL）
-- ============================================

CREATE TABLE t_tag (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(30) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted  SMALLINT    NOT NULL DEFAULT 0,
    CONSTRAINT uk_tag_name UNIQUE (name)
);

CREATE INDEX idx_tag_created_at ON t_tag (created_at);
CREATE INDEX idx_tag_is_deleted ON t_tag (is_deleted);

INSERT INTO t_tag (id, name, created_at, updated_at) VALUES
    (1, '性能优化', CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),
    (2, '配置排查', CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '7 days'),
    (3, '部署运维', CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '6 days'),
    (4, '权限认证', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (5, '前端体验', CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '4 days');

SELECT setval('t_tag_id_seq', (SELECT MAX(id) FROM t_tag));

INSERT INTO t_note_tag (note_id, tag_id) VALUES
    (1, 1),
    (2, 2),
    (2, 4),
    (3, 3),
    (4, 5),
    (5, 4),
    (5, 5);
