-- =====================================================================
-- V2: progress tracking + day planning (Phase 2)
--
-- progress        0-100 (%). completed is kept and stays in sync (completed = progress = 100),
--                 so "completed" filters keep working without recalculating.
-- due_date        the day the task is planned for. DATE (no time, no zone) = Java LocalDate.
-- postponed_count how many times the task was moved to a later day; used by the
--                 "this task is too big, split it" recommendation.
--
-- Flyway runs each V<n> file exactly once, in order, and records it in flyway_schema_history.
-- Never edit a migration that has already run — add a new V<n+1> instead.
-- =====================================================================

ALTER TABLE tasks
    ADD COLUMN progress        INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN due_date        DATE,
    ADD COLUMN postponed_count INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_tasks_progress_range CHECK (progress BETWEEN 0 AND 100),
    ADD CONSTRAINT chk_tasks_postponed_non_negative CHECK (postponed_count >= 0);

-- Existing completed tasks are 100% done.
UPDATE tasks SET progress = 100 WHERE completed;

-- Lets "tasks for a given day" queries use an index instead of scanning the whole table.
CREATE INDEX idx_tasks_due_date ON tasks (due_date);
