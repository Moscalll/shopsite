-- ShopSite DB init script
-- This file is executed by:
-- 1) MySQL Docker image on first container init (mounted to /docker-entrypoint-initdb.d)
-- 2) Spring Boot SQL initializer in some run profiles (classpath:db/init.sql)
--
-- Keep it SAFE + IDEMPOTENT.
-- The application uses JPA/Hibernate to manage tables, so this script should not
-- drop data or assume a fresh database.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

-- No-op statement to keep ScriptUtils happy if it expects at least one statement.
SELECT 1;
