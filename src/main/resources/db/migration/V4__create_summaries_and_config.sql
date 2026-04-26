CREATE TABLE weekly_summaries (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    week_start   DATE           NOT NULL,
    week_end     DATE           NOT NULL,
    summary_text TEXT           NOT NULL,
    total_spent  NUMERIC(10, 2),
    generated_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    provider     VARCHAR(20)
);

CREATE TABLE llm_provider_config (
    id              INT PRIMARY KEY DEFAULT 1,
    active_provider VARCHAR(20) NOT NULL DEFAULT 'ollama',
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

INSERT INTO llm_provider_config (active_provider) VALUES ('ollama');