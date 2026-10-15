-- =====================================================================
-- Sample data for the dev environment (do NOT run on production).
-- Run after Flyway has applied the migrations (start the app once):
--   docker exec -i todo-postgres psql -U todo -d tododb < backend/db/seed_dev_data.sql
-- =====================================================================

INSERT INTO tasks (title, description, completed) VALUES
    ('Learn Java core',        'Collections, Stream API, Optional', TRUE),
    ('Learn Spring Boot',      'Controller, Service, Repository, DTO', FALSE),
    ('Build the Task CRUD API', 'GET/POST/PUT/PATCH/DELETE /api/v1/tasks', FALSE),
    ('Set up the Vue frontend', NULL, FALSE);
