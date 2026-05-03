# Vault API

Spring Boot backend for personal finance tracking with PostgreSQL and Flyway.

## Summary

Vault supports:
- Expense tracking by category, linked to an account
- Income tracking by category, linked to an account
- Account management (Checking, Savings, Investment) with live balance calculation
- Investment accounts: optional metadata, checkpoint-based return tracking
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

- `src/main/java/com/vfa/vault/VaultApplication.java` — Spring Boot entry point
- `src/main/java/com/vfa/vault/controller/` — REST controllers:
	- `AccountController.java`
	- `CategoryController.java`
	- `ExpenseController.java`
	- `GoalController.java`
	- `IncomeCategoryController.java`
	- `IncomeController.java`
	- `WeeklySummaryController.java`
- `src/main/java/com/vfa/vault/service/` — Business logic/services:
	- `AccountService.java`
	- `CategoryService.java`
	- `ExpenseService.java`
	- `GoalService.java`
	- `IncomeCategoryService.java`
	- `IncomeService.java`
	- `InvestmentCheckpointService.java`
	- `WeeklySummaryService.java`
- `src/main/java/com/vfa/vault/repository/` — JPA repositories:
	- `AccountRepository.java`
	- `CategoryRepository.java`
	- `ExpenseRepository.java`
	- `GoalRepository.java`
	- `IncomeCategoryRepository.java`
	- `IncomeRepository.java`
	- `InvestmentCheckpointRepository.java`
	- `InvestmentDetailRepository.java`
	- `WeeklySummaryRepository.java`
- `src/main/java/com/vfa/vault/entity/` — JPA entities:
	- `Account.java`
	- `AccountType.java` (enum: `CHECKING`, `SAVINGS`, `INVESTMENT`)
	- `Category.java`
	- `Expense.java`
	- `Goal.java`
	- `Income.java`
	- `IncomeCategory.java`
	- `InvestmentCheckpoint.java`
	- `InvestmentDetail.java`
	- `WeeklySummary.java`
- `src/main/java/com/vfa/vault/dto/` — DTOs (request/response contracts):
	- `AccountDTO.java`
	- `CategoryDTO.java`
	- `ExpenseDTO.java`
	- `GoalDTO.java`
	- `IncomeCategoryDTO.java`
	- `IncomeDTO.java`
	- `InvestmentCheckpointDTO.java`
- `src/main/java/com/vfa/vault/exception/` — Exception handling:
	- `GlobalExceptionHandler.java`
	- `ResourceNotFoundException.java`
- `src/main/resources/application.yaml` — Main configuration
- `src/main/resources/db/migration/` — Flyway migrations (SQL):
	- `V1__create_categories.sql`
	- `V2__create_expenses.sql`
	- `V3__create_goals.sql`
	- `V4__create_summaries_and_config.sql`
	- `V5__create_accounts.sql`
	- `V6__create_investment_details.sql`
	- `V7__create_investment_checkpoints.sql`
	- `V8__create_income_categories.sql`
	- `V9__create_income.sql`
	- `V10__add_default_account.sql`
	- `V11__add_account_to_expenses.sql`

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
- `account_id` UUID NOT NULL REFERENCES accounts(id)
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

### accounts

- `id` UUID PRIMARY KEY DEFAULT gen_random_uuid()
- `name` VARCHAR(100) NOT NULL
- `account_type` ENUM (`CHECKING`, `SAVINGS`, `INVESTMENT`) NOT NULL
- `opening_balance` NUMERIC(10,2) NOT NULL DEFAULT 0
- `manual_balance` NUMERIC(10,2)
- `manual_balance_updated_at` TIMESTAMP
- `created_at` TIMESTAMP NOT NULL DEFAULT NOW()
- `is_active` BOOLEAN NOT NULL DEFAULT TRUE
- Soft-deleted (never hard-deleted)

### investment_details

- `id` UUID PRIMARY KEY DEFAULT gen_random_uuid()
- `account_id` UUID NOT NULL UNIQUE REFERENCES accounts(id)
- `platform` VARCHAR(100)
- `instrument` VARCHAR(100)
- `asset_type` VARCHAR(50)
- Optional — only created when investment metadata is provided

### investment_checkpoints

- `id` UUID PRIMARY KEY DEFAULT gen_random_uuid()
- `account_id` UUID NOT NULL REFERENCES accounts(id)
- `value` NUMERIC(10,2) NOT NULL
- `recorded_at` TIMESTAMP NOT NULL DEFAULT NOW()
- `note` VARCHAR(255)
- Indexes: `idx_checkpoints_account`, `idx_checkpoints_date`

### income_categories

- `id` SERIAL PRIMARY KEY
- `name` VARCHAR(50) NOT NULL UNIQUE
- `icon` VARCHAR(10)
- Seeded with: `Salary`, `Freelance`, `Dividend`, `Gift`, `Refund`, `Other`

### income

- `id` UUID PRIMARY KEY DEFAULT gen_random_uuid()
- `amount` NUMERIC(10,2) NOT NULL
- `note` VARCHAR(255)
- `income_category_id` INT NOT NULL REFERENCES income_categories(id)
- `account_id` UUID NOT NULL REFERENCES accounts(id)
- `income_date` DATE NOT NULL DEFAULT CURRENT_DATE
- `created_at` TIMESTAMP NOT NULL DEFAULT NOW()
- Indexes: `idx_income_date`, `idx_income_category`, `idx_income_account`

## API Reference

Base path: `/api/v1`

CORS origin:
- `http://localhost:3000`

### Categories

- `GET /categories` — list all categories

### Expenses

- `GET /expenses` — list expenses; optional query params: `month`, `categoryId`
- `POST /expenses` — create expense (requires `accountId`)
- `PUT /expenses/{id}` — update expense (requires `accountId`)
- `DELETE /expenses/{id}` — delete expense
- `GET /expenses/summary` — monthly summary; optional query param: `month`
- `GET /expenses/stats` — dashboard stats

### Goals

- `GET /goals` — list active goals
- `GET /goals/{id}` — get goal by id
- `POST /goals` — create goal
- `PUT /goals/{id}` — update goal
- `DELETE /goals/{id}` — deactivate goal
- `POST /goals/{id}/contribute` — add contribution
- `GET /goals/{id}/progress` — get progress

### Accounts

- `GET /accounts` — list all active accounts (includes live balance breakdown)
- `GET /accounts/{id}` — get account by id
- `POST /accounts` — create account
- `PUT /accounts/{id}` — update account
- `DELETE /accounts/{id}` — deactivate account (soft delete)
- `PATCH /accounts/{id}/manual-balance` — set manual balance override
- `GET /accounts/{id}/checkpoints` — list investment checkpoints (INVESTMENT type only)
- `POST /accounts/{id}/checkpoints` — add investment checkpoint (INVESTMENT type only)

### Income Categories

- `GET /income-categories` — list all income categories

### Income

- `GET /income` — list income; optional query params: `month`, `accountId`
- `POST /income` — create income entry
- `PUT /income/{id}` — update income entry
- `DELETE /income/{id}` — delete income entry
- `GET /income/summary` — monthly totals grouped by category; optional query param: `month`

## Balance Calculation

Account balances are **never stored** — always derived at query time:

```
calculatedBalance = openingBalance + totalIncome - totalExpenses
```

For **INVESTMENT** accounts, additional fields are derived:

| Field | Calculation |
|---|---|
| `contributedAmount` | same as `calculatedBalance` |
| `currentValue` | latest checkpoint value, or `contributedAmount` if no checkpoint |
| `returnAmount` | `currentValue - contributedAmount` |
| `returnPercentage` | `(returnAmount / contributedAmount) × 100` |

`manualBalance` is an optional override stored separately and does not affect the calculated balance — it is for display purposes only.

## Validation Rules

- Account name: required, max 100 chars
- Opening balance: `>= 0`
- Manual balance: `>= 0`
- Expense amount: `> 0`
- Expense note: max 255 chars
- `accountId` on expense and income: required
- Income amount: `> 0`
- Income note: max 255 chars
- `incomeCategoryId`: required
- `incomeDate`: required
- Goal target amount: `> 0`
- Contribution amount: `> 0`
- Goal name: required, max 100 chars
- Goal description: max 255 chars
- Checkpoint value: `> 0`

## Error Handling

All errors follow a consistent shape:

```json
{ "status": 404, "message": "Account not found with id: ...", "timestamp": "..." }
```

Validation errors return a map of field → message:

```json
{ "amount": "Amount must be greater than 0", "accountId": "Account is required" }
```

HTTP status codes:
- `404 Not Found` — resource does not exist
- `400 Bad Request` — validation failure or invalid operation (e.g. adding checkpoint to non-investment account)
- `500 Internal Server Error` — unexpected error

## Security Notes

- Do not commit real credentials in `.env`
- Keep secrets in environment variables/secret stores outside local development

