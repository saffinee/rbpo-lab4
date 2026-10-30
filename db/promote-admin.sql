\set ON_ERROR_STOP on
-- Запуск владельцем БД после обычной регистрации: psql parking_lab4 -v email='admin@example.com' -f db/promote-admin.sql
UPDATE users SET role = 'ADMIN' WHERE email = lower(:'email');
