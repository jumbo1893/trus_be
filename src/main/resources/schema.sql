CREATE SCHEMA IF NOT EXISTS codebook;

-- Limity TrusBota se řídí výhradně tabulkami membership. Původní ruční
-- konfigurace uživatele nesmí po nasazení dál ovlivňovat jeho členství.
DROP TABLE IF EXISTS ai_user_access;
DROP SEQUENCE IF EXISTS ai_user_access_seq;
