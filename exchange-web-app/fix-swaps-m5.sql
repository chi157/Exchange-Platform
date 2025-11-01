-- M5: Add per-user confirmation timestamp columns to swaps (MySQL 8)
ALTER TABLE `swaps`
  ADD COLUMN IF NOT EXISTS `a_confirmed_at` DATETIME(6) NULL,
  ADD COLUMN IF NOT EXISTS `b_confirmed_at` DATETIME(6) NULL;

-- Note: If your MySQL version does not support IF NOT EXISTS for ADD COLUMN,
-- run these individually and ignore errors if the column already exists:
-- ALTER TABLE `swaps` ADD COLUMN `a_confirmed_at` DATETIME(6) NULL;
-- ALTER TABLE `swaps` ADD COLUMN `b_confirmed_at` DATETIME(6) NULL;
