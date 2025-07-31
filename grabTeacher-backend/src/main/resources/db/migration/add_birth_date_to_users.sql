-- 添加出生年月字段到用户表
ALTER TABLE users ADD COLUMN birth_date VARCHAR(7) COMMENT '出生年月，格式：YYYY-MM';

-- 为现有用户设置默认值（可选）
-- UPDATE users SET birth_date = '1990-01' WHERE birth_date IS NULL;
