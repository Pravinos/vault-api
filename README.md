# Vault API

Spring Boot backend for personal finance tracking with PostgreSQL and Flyway.

## Overview

Vault is a personal finance API that supports:

- **Expense Tracking** – by category, linked to an account
- **Income Tracking** – by category, linked to an account
- **Account Management** – Checking, Savings, Investment with live balance calculation
- **Investment Accounts** – optional metadata with checkpoint-based return tracking
- **Financial Goals** – lifecycle management (create, update, contribute, deactivate)
- **Summaries & Stats** – monthly and weekly summaries with aggregate analytics

## Tech Stack

- **Java** 21
- **Spring Boot** 4.0.6
  - Spring Web MVC
  - Spring Data JPA
  - Spring Validation
- **Database** PostgreSQL
- **Migrations** Flyway
- **Build** Maven Wrapper

## Getting Started

### Prerequisites

- Java 21
- PostgreSQL (local or remote)

### Environment Setup

Create a `.env` file in the project root with:

```properties
DB_PASSWORD=your_postgres_password
```

### Running Locally

Using Maven Wrapper (PowerShell):

```powershell
# Start development server
.\mvnw.cmd spring-boot:run

# Compile only
.\mvnw.cmd compile

# Run tests
.\mvnw.cmd test
```

### Default Server

- http://localhost:8080

### Configuration

- **Runtime config** → [src/main/resources/application.yaml](src/main/resources/application.yaml)
- **Database** → PostgreSQL (configured in YAML)
- **.env import** → Loaded via Spring's `spring.config.import`
- **Flyway migrations** → Auto-run at startup with validation

## Project Structure

```
src/main/java/com/vfa/vault/
├── VaultApplication.java          # Spring Boot entry point
├── WebConfig.java                 # CORS & web configuration
├── controller/                    # REST API endpoints
│   ├── AccountController.java
│   ├── CategoryController.java
│   ├── ExpenseController.java
│   ├── GoalController.java
│   ├── IncomeCategoryController.java
│   ├── IncomeController.java
│   └── WeeklySummaryController.java
├── service/                       # Business logic & orchestration
│   ├── AccountService.java
│   ├── CategoryService.java
│   ├── ExpenseService.java
│   ├── GoalService.java
│   ├── IncomeCategoryService.java
│   ├── IncomeService.java
│   ├── InvestmentCheckpointService.java
│   └── WeeklySummaryService.java
├── repository/                    # JPA data access
│   ├── AccountRepository.java
│   ├── CategoryRepository.java
│   ├── ExpenseRepository.java
│   ├── GoalRepository.java
│   ├── IncomeCategoryRepository.java
│   ├── IncomeRepository.java
│   ├── InvestmentCheckpointRepository.java
│   ├── InvestmentDetailRepository.java
│   └── WeeklySummaryRepository.java
├── entity/                        # JPA entity models
│   ├── Account.java
│   ├── AccountType.java           # Enum: CHECKING, SAVINGS, INVESTMENT
│   ├── Category.java
│   ├── Expense.java
│   ├── Goal.java
│   ├── Income.java
│   ├── IncomeCategory.java
│   ├── InvestmentCheckpoint.java
│   ├── InvestmentDetail.java
│   └── WeeklySummary.java
├── dto/                           # Request/Response contracts
│   ├── AccountDTO.java
│   ├── CategoryDTO.java
│   ├── ExpenseDTO.java
│   ├── GoalDTO.java
│   ├── IncomeCategoryDTO.java
│   ├── IncomeDTO.java
│   └── InvestmentCheckpointDTO.java
└── exception/                     # Error handling
    ├── GlobalExceptionHandler.java
    └── ResourceNotFoundException.java

src/main/resources/
├── application.yaml               # Spring Boot configuration
├── db/migration/                  # Flyway SQL migrations
│   ├── V1__create_categories.sql
│   ├── V2__create_expenses.sql
│   ├── V3__create_goals.sql
│   ├── V4__create_summaries_and_config.sql
│   ├── V5__create_accounts.sql
│   ├── V6__create_investment_details.sql
│   ├── V7__create_investment_checkpoints.sql
│   ├── V8__create_income_categories.sql
│   ├── V9__create_income.sql
│   ├── V10__add_default_account.sql
│   └── V11__add_account_to_expenses.sql
└── templates/                     # Static resources
```

## Database Schema

### Categories

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | SERIAL | PRIMARY KEY |
| `name` | VARCHAR(50) | NOT NULL, UNIQUE |
| `icon` | VARCHAR(10) | — |

*Seeded with default rows in [V1__create_categories.sql](src/main/resources/db/migration/V1__create_categories.sql)*

### Expenses

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() |
| `amount` | NUMERIC(10,2) | NOT NULL |
| `note` | VARCHAR(255) | — |
| `category_id` | INT | NOT NULL, REFERENCES categories(id) |
| `account_id` | UUID | NOT NULL, REFERENCES accounts(id) |
| `expense_date` | DATE | NOT NULL, DEFAULT CURRENT_DATE |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

*Indexes: `idx_expenses_date`, `idx_expenses_category`*

### Goals

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() |
| `name` | VARCHAR(100) | NOT NULL |
| `description` | VARCHAR(255) | — |
| `target_amount` | NUMERIC(10,2) | NOT NULL |
| `saved_amount` | NUMERIC(10,2) | NOT NULL, DEFAULT 0 |
| `goal_type` | ENUM | `SHORT_TERM`, `LONG_TERM` |
| `deadline` | DATE | — |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE |

### Weekly Summaries

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() |
| `week_start` | DATE | NOT NULL |
| `week_end` | DATE | NOT NULL |
| `summary_text` | TEXT | NOT NULL |
| `total_spent` | NUMERIC(10,2) | — |
| `generated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| `provider` | VARCHAR(20) | — |

### LLM Provider Config

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | INT | PRIMARY KEY, DEFAULT 1 |
| `active_provider` | VARCHAR(20) | NOT NULL, DEFAULT `ollama` |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

### Accounts

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() |
| `name` | VARCHAR(100) | NOT NULL |
| `account_type` | ENUM | NOT NULL: `CHECKING`, `SAVINGS`, `INVESTMENT` |
| `opening_balance` | NUMERIC(10,2) | NOT NULL, DEFAULT 0 |
| `manual_balance` | NUMERIC(10,2) | — |
| `manual_balance_updated_at` | TIMESTAMP | — |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE |

*Note: Soft-deleted (never hard-deleted)*

### Investment Details

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() |
| `account_id` | UUID | NOT NULL, UNIQUE, REFERENCES accounts(id) |
| `platform` | VARCHAR(100) | — |
| `instrument` | VARCHAR(100) | — |
| `asset_type` | VARCHAR(50) | — |

*Optional — only created when investment metadata is provided*

### Investment Checkpoints

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() |
| `account_id` | UUID | NOT NULL, REFERENCES accounts(id) |
| `value` | NUMERIC(10,2) | NOT NULL |
| `recorded_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| `note` | VARCHAR(255) | — |

*Indexes: `idx_checkpoints_account`, `idx_checkpoints_date`*

### Income Categories

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | SERIAL | PRIMARY KEY |
| `name` | VARCHAR(50) | NOT NULL, UNIQUE |
| `icon` | VARCHAR(10) | — |

*Seeded with: Salary, Freelance, Dividend, Gift, Refund, Other*

### Income

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() |
| `amount` | NUMERIC(10,2) | NOT NULL |
| `note` | VARCHAR(255) | — |
| `income_category_id` | INT | NOT NULL, REFERENCES income_categories(id) |
| `account_id` | UUID | NOT NULL, REFERENCES accounts(id) |
| `income_date` | DATE | NOT NULL, DEFAULT CURRENT_DATE |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |

*Indexes: `idx_income_date`, `idx_income_category`, `idx_income_account`*

## API Reference

**Base Path:** `/api/v1`

**CORS:** Allowed origins include `http://localhost:3000`

### Categories

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/categories` | List all categories |

### Expenses

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/expenses` | List expenses (optional params: `month`, `categoryId`) |
| POST | `/expenses` | Create expense (requires `accountId`) |
| PUT | `/expenses/{id}` | Update expense (requires `accountId`) |
| DELETE | `/expenses/{id}` | Delete expense |
| GET | `/expenses/summary` | Monthly expense summary (optional param: `month`) |
| GET | `/expenses/stats` | Dashboard statistics |

### Goals

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/goals` | List active goals |
| GET | `/goals/{id}` | Get goal by ID |
| POST | `/goals` | Create new goal |
| PUT | `/goals/{id}` | Update goal |
| DELETE | `/goals/{id}` | Deactivate goal |
| POST | `/goals/{id}/contribute` | Add contribution to goal |
| GET | `/goals/{id}/progress` | Get goal progress |

### Accounts

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/accounts` | List all active accounts (includes live balance breakdown) |
| GET | `/accounts/{id}` | Get account by ID |
| POST | `/accounts` | Create account |
| PUT | `/accounts/{id}` | Update account |
| DELETE | `/accounts/{id}` | Deactivate account (soft delete) |
| PATCH | `/accounts/{id}/manual-balance` | Set manual balance override |
| GET | `/accounts/{id}/checkpoints` | List investment checkpoints (INVESTMENT type only) |
| POST | `/accounts/{id}/checkpoints` | Add investment checkpoint (INVESTMENT type only) |

### Income Categories

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/income-categories` | List all income categories |

### Income

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/income` | List income (optional params: `month`, `accountId`) |
| POST | `/income` | Create income entry |
| PUT | `/income/{id}` | Update income entry |
| DELETE | `/income/{id}` | Delete income entry |
| GET | `/income/summary` | Monthly income totals by category (optional param: `month`) |

## Balance Calculation

Account balances are **never stored** — always calculated at query time:

$$\text{Calculated Balance} = \text{Opening Balance} + \text{Total Income} - \text{Total Expenses}$$

### Investment Account Fields

For **INVESTMENT** type accounts, the following derived fields are included:

| Field | Calculation |
|-------|-------------|
| `contributedAmount` | Same as calculated balance |
| `currentValue` | Latest checkpoint value, or `contributedAmount` if no checkpoint exists |
| `returnAmount` | `currentValue - contributedAmount` |
| `returnPercentage` | $\frac{\text{returnAmount}}{\text{contributedAmount}} \times 100$ |

**Note:** `manualBalance` is an optional override stored separately and does not affect the calculated balance — it is for display purposes only.

## Validation Rules

### Account

- `name`: Required, max 100 characters
- `openingBalance`: ≥ 0
- `manualBalance`: ≥ 0

### Expense

- `amount`: > 0
- `note`: max 255 characters
- `accountId`: Required
- `categoryId`: Required (implicit through category)

### Income

- `amount`: > 0
- `note`: max 255 characters
- `accountId`: Required
- `incomeCategoryId`: Required
- `incomeDate`: Required

### Goal

- `name`: Required, max 100 characters
- `description`: max 255 characters
- `targetAmount`: > 0
- `contribution`: > 0 (when adding)
- `deadline`: optional but must be future date if provided

### Investment Checkpoint

- `value`: > 0

## Error Handling

All API errors follow a consistent format:

### Standard Error Response

```json
{
  "status": 404,
  "message": "Account not found with id: ...",
  "timestamp": "2026-05-03T10:30:00Z"
}
```

### Validation Error Response

For validation failures, a map of field-to-message is returned:

```json
{
  "amount": "Amount must be greater than 0",
  "accountId": "Account is required"
}
```

### HTTP Status Codes

| Status | Description |
|--------|-------------|
| `400` | Bad Request — validation failure or invalid operation |
| `404` | Not Found — resource does not exist |
| `500` | Internal Server Error — unexpected error |

## Security

⚠️ **Important:**

- Never commit real credentials in `.env`
- Always use environment variables or secret stores for credentials
- In production, use a proper secrets management solution (AWS Secrets Manager, HashiCorp Vault, etc.)
- Keep `.env` out of version control (add to `.gitignore`)

