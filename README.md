# Vault API

Spring Boot backend for personal finance tracking with PostgreSQL, Flyway, and password-gate authentication.

## Overview

Vault is a personal finance API protected by a single vault password with JWT-based authentication. Features include:

- **Password Gate** – single vault password, no user registration, HTTP-only cookie-based JWT auth
- **Rate Limiting** – 5 login/setup attempts per 15 minutes per IP
- **Expense Tracking** – by category, linked to an account
- **Income Tracking** – by category, linked to an account
- **Transfers** – account-to-account transfers with one-time reversal support
- **Account Management** – Checking, Savings, Investment with live balance calculation
- **Unified Dashboard API** – `GET /api/v1/dashboard` returns pre-calculated dashboard metrics
- **Investment Accounts** – optional metadata with checkpoint-based return tracking
- **Financial Goals** – lifecycle management (create, update, contribute, deactivate)
- **Summaries & Stats** – monthly and weekly summaries with aggregate analytics
- **AI Assistant** – chat over real finance data with tool-calling
- **LLM Routing** – per-task provider/model selection for chat and summaries
- **Model Discovery** – live LM Studio and Groq model list endpoints
- **Manual AI Summary Generation** – trigger summary generation from API

## Tech Stack

- **Java** 21
- **Spring Boot** 4.0.6
  - Spring Web MVC
  - Spring Data JPA
  - Spring Validation
  - Spring Security 7.0.5
  - Spring AI (OpenAI-compatible clients)
- **Authentication** JWT (JJWT 0.12.6), BCrypt, HttpOnly Cookies
- **Rate Limiting** Bucket4j 8.10.1 (5 attempts per 15 minutes per IP)
- **Database** PostgreSQL
- **Migrations** Flyway 11.14.1
- **Build** Maven Wrapper

## Getting Started

### Prerequisites

- Java 21
- PostgreSQL (local or remote)

### Environment Setup

Create a `.env` file in the project root with:

```properties
# Database
DB_PASSWORD=your_postgres_password

# JWT & Auth (Required for production)
VAULT_JWT_SECRET=your-long-random-secret-min-32-chars
VAULT_COOKIE_SECURE=true          # false in local dev, true in production
VAULT_COOKIE_SAME_SITE=None       # "None" for cross-origin (Vercel), "Strict" for same-site
FRONTEND_URL=https://your-frontend.vercel.app

# AI Providers (Optional)
GROQ_API_KEY=your_groq_api_key
OPENAI_API_KEY=lm-studio
```

**Important:** `VAULT_JWT_SECRET` must be at least 32 characters of random data (use `openssl rand -base64 32` to generate). This is the signing key for all JWT tokens.

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

### First-Time Setup

On first run, the vault is unconfigured. Use the frontend to:

1. Check `/api/v1/auth/status` → returns `{"configured": false}`
2. Call `/api/v1/auth/setup` with your chosen password (min 8 chars)
3. Password is hashed with BCrypt and stored in the database
4. Receive a JWT token in an HttpOnly cookie
5. All subsequent requests include the cookie automatically

### Configuration

- **Runtime config** → [src/main/resources/application.yaml](src/main/resources/application.yaml)
- **Database** → PostgreSQL (configured in YAML)
- **.env import** → Loaded via Spring's `spring.config.import`
- **Flyway migrations** → Auto-run at startup with validation
- **Auth config** → `vault.auth.*` in YAML (JWT secret, cookie settings, CORS frontend URL)

## Project Structure

```
src/main/java/com/vfa/vault/
├── VaultApplication.java          # Spring Boot entry point
├── WebConfig.java                 # CORS & web configuration
├── ai/                            # AI integration layer
│   ├── AiConfig.java
│   ├── FinanceTools.java
│   ├── LlmProviderRouter.java
│   └── ModelDiscoveryService.java
├── controller/                    # REST API endpoints
│   ├── AccountController.java
│   ├── AiController.java
│   ├── CategoryController.java
│   ├── ExpenseController.java
│   ├── GoalController.java
│   ├── IncomeCategoryController.java
│   ├── IncomeController.java
│   ├── TransferController.java
│   ├── DashboardController.java
│   └── WeeklySummaryController.java
├── service/                       # Business logic & orchestration
│   ├── AccountService.java
│   ├── AccountBalanceService.java
│   ├── CategoryService.java
│   ├── ExpenseService.java
│   ├── GoalService.java
│   ├── IncomeCategoryService.java
│   ├── IncomeService.java
│   ├── InvestmentCheckpointService.java
│   ├── DashboardService.java
│   ├── TransferService.java
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
│   ├── LlmProviderConfigRepository.java
│   ├── TransferRepository.java
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
│   ├── LlmProviderConfig.java
│   ├── Transfer.java
│   └── WeeklySummary.java
├── config/                        # Auth, security, and utility beans
│   ├── SecurityConfig.java        # Spring Security filter chain, CORS
│   ├── JwtUtil.java               # JWT generation and validation
│   ├── JwtFilter.java             # Extracts JWT from cookie, sets SecurityContext
│   ├── RateLimitFilter.java       # Rate limiting: 5 attempts per 15 min per IP
│   └── CookieUtil.java            # Builds/clears HttpOnly cookies with SameSite policy
├── controller/                    # REST API endpoints
│   ├── AuthController.java        # /api/v1/auth/* (setup, login, verify, refresh, logout, status)
│   ├── AccountController.java
│   ├── AiController.java
│   ├── CategoryController.java
│   ├── ExpenseController.java
│   ├── GoalController.java
│   ├── IncomeCategoryController.java
│   ├── IncomeController.java
│   └── WeeklySummaryController.java
├── dto/                           # Request/Response contracts
│   ├── AccountDTO.java
│   ├── AiConfigResponseDTO.java
│   ├── AiConfigUpdateDTO.java
│   ├── CategoryDTO.java
│   ├── ChatRequestDTO.java
│   ├── ChatResponseDTO.java
│   ├── ExpenseDTO.java
│   ├── GoalDTO.java
│   ├── IncomeCategoryDTO.java
│   ├── IncomeDTO.java
│   ├── InvestmentCheckpointDTO.java
│   ├── AccountDashboardDTO.java
│   ├── DashboardResponseDTO.java
│   ├── TransferDTO.java
│   ├── TransferResponseDTO.java
│   └── WeeklySummaryDTO.java
├── entity/                        # JPA entity models
│   ├── AppConfig.java             # Key-value store for vault configuration (password hash)
│   ├── Account.java
│   ├── Category.java
│   ├── Expense.java
│   ├── Goal.java
│   ├── Income.java
│   ├── IncomeCategory.java
│   ├── InvestmentCheckpoint.java
│   ├── InvestmentDetail.java
│   ├── LlmProviderConfig.java
│   ├── Transfer.java
│   └── WeeklySummary.java
└── exception/                     # Error handling
    ├── GlobalExceptionHandler.java
    └── ResourceNotFoundException.java

src/main/resources/
├── application.yaml               # Spring Boot configuration
├── db/migration/                  # Flyway SQL migrations (18 versions)
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
│   ├── V11__add_account_to_expenses.sql
│   ├── V12__expand_llm_provider_config.sql
│   ├── V13__add_model_to_weekly_summaries.sql
│   ├── V14__create_app_config.sql
│   ├── V15__set_groq_llama_defaults.sql
│   ├── V16__remove_soft_delete_from_accounts.sql
│   ├── V17__create_transfers.sql
│   └── V18__transfer_reversal_guards.sql
└── templates/                     # Static resources
```

## Authentication

### Password Gate Model

Vault uses a **single shared password** to protect all data. There is no user registration or multi-user support:

- **First-time setup**: Call `/api/v1/auth/setup` with your chosen password (min 8 chars)
- **Password hashing**: BCrypt with automatic salt generation
- **JWT tokens**: 24-hour expiry, signed with HMAC SHA-256
- **Storage**: JWT is stored in an **HttpOnly cookie** (not accessible to JavaScript)
- **CORS support**: Cookies work cross-origin with `SameSite=None; Secure` policy

### Authentication Flow

```
1. Frontend checks /api/v1/auth/status
   └─ If configured=false, show setup form
   └─ If configured=true, redirect to login

2. Setup (first time)
   POST /api/v1/auth/setup { password: "my-password" }
   ├─ Password hashed with BCrypt
   ├─ Hash stored in app_config table
   └─ Returns Set-Cookie with JWT

3. Login (subsequent times)
   POST /api/v1/auth/login { password: "my-password" }
   ├─ Password compared against stored hash
   └─ On match, returns Set-Cookie with JWT
   └─ Rate limited: 5 attempts per 15 minutes per IP

4. Authenticated requests
   GET /api/v1/expenses
   ├─ Browser automatically includes cookie
   ├─ JwtFilter extracts token from cookie
   ├─ Validates JWT signature and expiry
   └─ If valid, request proceeds; if expired, return 401

5. Refresh
   POST /api/v1/auth/refresh
   ├─ Requires valid JWT in cookie
   └─ Issues new 24-hour token

6. Logout
   POST /api/v1/auth/logout
   └─ Clears cookie (maxAge=0)
```

### Rate Limiting

- **Endpoints protected**: `/api/v1/auth/setup` and `/api/v1/auth/login`
- **Limit**: 5 attempts per 15 minutes per client IP
- **IP detection**: Proxy-aware (checks `X-Forwarded-For` and `X-Real-IP` headers)
- **On limit exceeded**: HTTP 429 with error message
- **Implementation**: Bucket4j token bucket algorithm per IP

### Deployment Notes

**For local development:**
- `VAULT_COOKIE_SECURE=false` (cookies work over HTTP)
- `VAULT_COOKIE_SAME_SITE=Strict` (cookies only sent to same domain)

**For production (Render backend + Vercel frontend):**
- `VAULT_COOKIE_SECURE=true` (cookies only sent over HTTPS)
- `VAULT_COOKIE_SAME_SITE=None` (cookies sent in cross-origin requests from Vercel)
- `FRONTEND_URL=https://your-app.vercel.app` (CORS origin)
- `VAULT_JWT_SECRET` must be a random 32+ character string

## Database Schema

### AppConfig

| Column | Type | Constraints |
|--------|------|-------------|  
| `key` | VARCHAR(100) | PRIMARY KEY |
| `value` | TEXT | NOT NULL |

*Stores configuration as key-value pairs. Currently used to store the vault password hash with key=`vault_password_hash`.*

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
| `chat_provider` | VARCHAR(20) | NOT NULL, DEFAULT `lmstudio` |
| `chat_model` | VARCHAR(100) | NOT NULL, DEFAULT `mistral-7b-instruct` |
| `summary_provider` | VARCHAR(20) | NOT NULL, DEFAULT `groq` |
| `summary_model` | VARCHAR(100) | NOT NULL, DEFAULT `llama3-70b-8192` |
| `lmstudio_models` | TEXT | nullable, JSON array cache |
| `groq_models` | TEXT | nullable, JSON array cache |
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

*Note: `is_active` was removed in V16. Accounts are hard-deleted if no FK references exist.*

### Transfers

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() |
| `from_account_id` | UUID | NOT NULL, REFERENCES accounts(id) |
| `to_account_id` | UUID | NOT NULL, REFERENCES accounts(id) |
| `amount` | NUMERIC(10,2) | NOT NULL |
| `note` | VARCHAR(255) | — |
| `transfer_date` | DATE | NOT NULL, DEFAULT CURRENT_DATE |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() |
| `original_transfer_id` | UUID | NULL, REFERENCES transfers(id) |
| `is_reversal` | BOOLEAN | NOT NULL, DEFAULT FALSE |

*Indexes: `idx_transfers_from`, `idx_transfers_to`, `ux_transfers_original_transfer_id`*

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

**Authentication:** All endpoints except `/auth/status`, `/auth/setup`, and `/auth/login` require a valid JWT in an HttpOnly cookie.

**CORS:** Configured to allow the frontend URL specified in `FRONTEND_URL` environment variable with credentials (`allowCredentials=true`).

### Authentication

| Method | Endpoint | Description | Public? |
|--------|----------|-------------|----------|
| GET | `/auth/status` | Check if vault is configured | Yes |
| POST | `/auth/setup` | Configure vault with password (first-time only) | Yes* |
| POST | `/auth/login` | Authenticate with vault password | Yes* |
| GET | `/auth/verify` | Verify JWT is valid (heartbeat) | No |
| POST | `/auth/refresh` | Issue new JWT token | No |
| POST | `/auth/logout` | Clear authentication cookie | No |

*Rate limited: 5 attempts per 15 minutes per IP

**GET /auth/status — response:**
```json
{
  "configured": true
}
```

**POST /auth/setup — request:**
```json
{
  "password": "my-vault-password"
}
```

**POST /auth/setup — response (on success):**
```json
{
  "message": "Vault configured successfully"
}
```
*Sets `Set-Cookie` header with HttpOnly JWT token*

**POST /auth/login — request:**
```json
{
  "password": "my-vault-password"
}
```

**POST /auth/login — response (on success):**
```json
{
  "message": "Login successful"
}
```
*Sets `Set-Cookie` header with HttpOnly JWT token*

**GET /auth/verify — response:**
```json
{
  "valid": true
}
```

**POST /auth/refresh — response:**
```json
{
  "message": "Token refreshed"
}
```
*Sets `Set-Cookie` header with new HttpOnly JWT token*

**POST /auth/logout — response:**
```json
{
  "message": "Logged out"
}
```
*Clears authentication cookie*

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
| GET | `/accounts` | List all accounts (includes live balance breakdown) |
| GET | `/accounts/{id}` | Get account by ID |
| POST | `/accounts` | Create account |
| PUT | `/accounts/{id}` | Update account |
| `DELETE` | `/accounts/{id}` | Delete account (fails when referenced by transactions/checkpoints) |
| PATCH | `/accounts/{id}/manual-balance` | Set manual balance override |
| GET | `/accounts/{id}/checkpoints` | List investment checkpoints (INVESTMENT type only) |
| POST | `/accounts/{id}/checkpoints` | Add investment checkpoint (INVESTMENT type only) |
| `GET` | `/accounts/{id}/transfers` | List account transfer history |

### Dashboard

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/dashboard` | Single-source dashboard payload with net worth, account balances, month stats, and MoM deltas |

### Transfers

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/transfers` | Create transfer between two existing accounts |
| `POST` | `/transfers/{id}/revert` | Create opposite transfer (one-time reversal) |

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

### Weekly Summaries

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/summaries` | List weekly summaries (newest first) |
| GET | `/summaries/latest` | Get most recent weekly summary |
| GET | `/summaries/{id}` | Get weekly summary by id |
| DELETE | `/summaries/{id}` | Delete weekly summary (idempotent hard delete) |

### AI

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/ai/chat` | Chat with AI using finance tool-calling |
| GET | `/ai/config` | Get chat/summary provider + model config |
| PATCH | `/ai/config` | Update provider/model for `chat` or `summary` task |
| GET | `/ai/models/lmstudio` | Discover available LM Studio models |
| GET | `/ai/models/groq` | Discover available Groq models |
| POST | `/ai/summaries/generate` | Manually generate and save weekly AI summary |

## AI Integration

- `Chat` and `Summary` can each use independent provider/model combinations.
- Providers currently supported: `lmstudio`, `groq`.
- Chat requests support optional `conversationId` for memory continuity.
- Weekly summary generation logs each step and returns readable error payloads on failure.
- Model discovery responses are cached in `llm_provider_config`.
- `FinanceTools.getDashboardSummary()` uses `DashboardService.getDashboard()` so AI and dashboard use identical calculations.

## Balance Calculation

Account balances are **never stored** — always calculated at query time:

$$\text{Calculated Balance} = \text{Opening Balance} + \text{Total Income} - \text{Total Expenses} + \text{Incoming Transfers} - \text{Outgoing Transfers}$$

### Investment Account Fields

For **INVESTMENT** type accounts, the following derived fields are included:

| Field | Calculation |
|-------|-------------|
| `contributedAmount` | Same as calculated balance |
| `currentValue` | Manual snapshot if present (checkpoint-aligned), otherwise latest checkpoint value, otherwise `contributedAmount` |
| `returnAmount` | `currentValue - contributedAmount` |
| `returnPercentage` | $\frac{\text{returnAmount}}{\text{contributedAmount}} \times 100$ |

**Note:** For investment accounts, the primary displayed account balance follows the latest manual/checkpoint snapshot (`currentValue`). Transfers touching investment accounts also adjust this snapshot so the main balance reflects incoming/outgoing moves immediately.

## Dashboard Aggregation

Dashboard metrics are computed server-side in `DashboardService` and exposed via `GET /api/v1/dashboard`.

- Net worth, monthly totals, top category, and MoM percentages are pre-calculated in one backend response.
- Account cards are pre-computed (including investment return fields and display labels).
- The AI tool `getDashboardSummary()` reuses the same service output to prevent calculation drift.

**Transfer validation note:** accounts are validated by existence. The `accounts` table has no active/inactive flag after V16.

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

⚠️ **Critical Security Requirements:**

- **JWT Secret** (`VAULT_JWT_SECRET`): Must be at least 32 random characters. Generate with `openssl rand -base64 32`. Never hardcode or commit this value.
- **Cookie Security**: In production, always set `VAULT_COOKIE_SECURE=true` to ensure cookies are only sent over HTTPS.
- **Password Policy**: Minimum 8 characters enforced. Consider prompting users for stronger passwords (12+ chars, mixed case, symbols).
- **Rate Limiting**: Protect `/setup` and `/login` from brute-force attacks (default: 5 attempts per 15 minutes per IP).
- **HTTPS Only**: Deploy on HTTPS (Render and Vercel both enforce this). JWT tokens in cookies require HTTPS + Secure flag.
- **Environment Variables**: Never commit `.env` files. Use your platform's secret management (Render environment variables, Vercel secrets).
- **CORS Configuration**: Only allow your frontend domain. Adjust `FRONTEND_URL` per deployment environment.

**Deployment checklist:**
- [ ] `VAULT_JWT_SECRET` is 32+ random characters
- [ ] `VAULT_COOKIE_SECURE=true` on production
- [ ] `VAULT_COOKIE_SAME_SITE=None` if frontend is cross-origin
- [ ] `FRONTEND_URL` matches your frontend deployment domain
- [ ] HTTPS is enforced (both backend and frontend)
- [ ] Database backups enabled (Supabase settings)
- [ ] No console.log() statements exposing sensitive data in frontend code

