ALTER TABLE llm_provider_config
    ADD COLUMN IF NOT EXISTS chat_provider VARCHAR(20),
    ADD COLUMN IF NOT EXISTS chat_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS summary_provider VARCHAR(20),
    ADD COLUMN IF NOT EXISTS summary_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS lmstudio_models TEXT,
    ADD COLUMN IF NOT EXISTS groq_models TEXT;

UPDATE llm_provider_config
SET chat_provider = CASE
        WHEN active_provider = 'groq' THEN 'groq'
        ELSE 'lmstudio'
    END
WHERE chat_provider IS NULL;

UPDATE llm_provider_config
SET summary_provider = 'groq'
WHERE summary_provider IS NULL;

UPDATE llm_provider_config
SET chat_model = 'mistral-7b-instruct'
WHERE chat_model IS NULL;

UPDATE llm_provider_config
SET summary_model = 'llama3-70b-8192'
WHERE summary_model IS NULL;

ALTER TABLE llm_provider_config
    ALTER COLUMN chat_provider SET DEFAULT 'lmstudio',
    ALTER COLUMN chat_provider SET NOT NULL,
    ALTER COLUMN chat_model SET DEFAULT 'mistral-7b-instruct',
    ALTER COLUMN chat_model SET NOT NULL,
    ALTER COLUMN summary_provider SET DEFAULT 'groq',
    ALTER COLUMN summary_provider SET NOT NULL,
    ALTER COLUMN summary_model SET DEFAULT 'llama3-70b-8192',
    ALTER COLUMN summary_model SET NOT NULL;
