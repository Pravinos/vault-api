# Vault API

Spring Boot backend for personal finance tracking with PostgreSQL and Flyway.

## Summary

Vault supports:
- Expense tracking by category
- Monthly summaries and aggregate stats
- Savings goal lifecycle (create, update, contribute, deactivate)
- Weekly summary storage

Stack:
- Java 21
- Spring Boot 4 (Web MVC, Validation, Data JPA)
- PostgreSQL
- Flyway
- Maven Wrapper

## Quick Start

Prerequisites:
- Java 21
- PostgreSQL

Required configuration:
- `DB_PASSWORD` in a root `.env` file

Run locally (PowerShell):
- `.\mvnw.cmd spring-boot:run`

Build and test:
- `.\mvnw.cmd compile`
- `.\mvnw.cmd test`

Default server:
- http://localhost:8080

## Configuration Notes

- Runtime config is in `src/main/resources/application.yaml`.
- `.env` is imported through Spring `spring.config.import`.
- Default datasource URL and username are already set in YAML.
- Flyway runs automatically at startup and validates migrations.

## Project Layout

- `src/main/java/com/vfa/vault/controller` REST controllers
- `src/main/java/com/vfa/vault/service` business logic
- `src/main/java/com/vfa/vault/repository` JPA repositories
- `src/main/java/com/vfa/vault/entity` entities
- `src/main/java/com/vfa/vault/dto` request/response contracts
- `src/main/resources/db/migration` Flyway migrations

## Database Tables

### categories

- `id` SERIAL PRIMARY KEY
- `name` VARCHAR(50) NOT NULL UNIQUE
- `icon` VARCHAR(10)
- Seeded with default rows in `V1__create_categories.sql`

### expenses

- `id` UUID PRIMARY KEY DEFAULT gen_random_uuid()
- `amount` NUMERIC(10,2) NOT NULL
- `note` VARCHAR(255)
- `category_id` INT NOT NULL REFERENCES categories(id)
- `expense_date` DATE NOT NULL DEFAULT CURRENT_DATE
- `created_at` TIMESTAMP NOT NULL DEFAULT NOW()
- Indexes: `idx_expenses_date`, `idx_expenses_category`

### goals

- `id` UUID PRIMARY KEY DEFAULT gen_random_uuid()
- `name` VARCHAR(100) NOT NULL
- `description` VARCHAR(255)
- `target_amount` NUMERIC(10,2) NOT NULL
- `saved_amount` NUMERIC(10,2) NOT NULL DEFAULT 0
- `goal_type` ENUM (`SHORT_TERM`, `LONG_TERM`)
- `deadline` DATE
- `created_at` TIMESTAMP NOT NULL DEFAULT NOW()
- `is_active` BOOLEAN NOT NULL DEFAULT TRUE

### weekly_summaries

- `id` UUID PRIMARY KEY DEFAULT gen_random_uuid()
- `week_start` DATE NOT NULL
- `week_end` DATE NOT NULL
- `summary_text` TEXT NOT NULL
- `total_spent` NUMERIC(10,2)
- `generated_at` TIMESTAMP NOT NULL DEFAULT NOW()
- `provider` VARCHAR(20)

### llm_provider_config

- `id` INT PRIMARY KEY DEFAULT 1
- `active_provider` VARCHAR(20) NOT NULL DEFAULT `ollama`
- `updated_at` TIMESTAMP NOT NULL DEFAULT NOW()

## API Reference

Base path: `/api/v1`

CORS origin:
- `http://localhost:3000`

### Categories

- `GET /categories` list all categories

### Expenses

- `GET /expenses` list expenses, optional query params: `month`, `categoryId`
- `POST /expenses` create expense
- `PUT /expenses/{id}` update expense
- `DELETE /expenses/{id}` delete expense
- `GET /expenses/summary` monthly summary, optional query param: `month`
- `GET /expenses/stats` dashboard stats

### Goals

- `GET /goals` list active goals
- `GET /goals/{id}` get goal by id
- `POST /goals` create goal
- `PUT /goals/{id}` update goal
- `DELETE /goals/{id}` deactivate goal
- `POST /goals/{id}/contribute` add contribution
- `GET /goals/{id}/progress` get progress

## Validation Rules

- Expense amount must be greater than 0
- Goal target amount must be greater than 0
- Contribution amount must be greater than 0
- Goal name is required, max 100 chars
- Description max 255 chars
- Expense note max 255 chars

## Security Notes

- Do not commit real credentials in `.env`
- Keep secrets in environment variables/secret stores outside local development
