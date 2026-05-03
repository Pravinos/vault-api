# Vault — Personal Finance Assistant
### Architecture, DB Schema, API Endpoints & Implementation Guide

---

## Table of Contents
1. [Tech Stack](#tech-stack)
2. [Architecture Overview](#architecture-overview)
3. [Database Schema](#database-schema)
4. [API Endpoints](#api-endpoints)
5. [AI Integration](#ai-integration)
6. [Implementation Phases](#implementation-phases)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.x |
| AI Framework | Spring AI |
| Local LLM | Ollama (Mistral 7B / LLaMA 3) |
| Cloud LLM | Groq API (llama3-70b-8192 — free tier) |
| Database | PostgreSQL 16 |
| Frontend | Next.js (App Router) |
| Auth | Spring Security + JWT |
| Build | Maven |

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                        Next.js Frontend                          │
│  Dashboard │ Accounts │ Expenses │ Income │ Goals │ Chat UI      │
└───────────────────────────┬──────────────────────────────────────┘
                            │ REST / JSON
┌───────────────────────────▼──────────────────────────────────────┐
│                     Spring Boot Backend                          │
│                                                                  │
│  ┌─────────────┐   ┌──────────────┐   ┌────────────────────┐    │
│  │  REST API   │   │  AI Service  │   │     Scheduler      │    │
│  │ Controllers │   │  Spring AI   │   │  Weekly Summary    │    │
│  └──────┬──────┘   └──────┬───────┘   └─────────┬──────────┘    │
│         │                 │                      │               │
│  ┌──────▼─────────────────▼──────────────────────▼───────────┐   │
│  │                  LLM Provider Router                      │   │
│  │       Ollama (local)  │  Groq API (cloud fallback)        │   │
│  └───────────────────────────────────────────────────────────┘   │
└───────────────────────────┬──────────────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────────┐
│                         PostgreSQL                               │
│                                                                  │
│  accounts │ investment_details │ investment_checkpoints          │
│  expenses │ categories                                           │
│  income   │ income_categories                                    │
│  goals    │ weekly_summaries   │ llm_provider_config             │
└──────────────────────────────────────────────────────────────────┘
```

---

## Database Schema

> Migrations are managed by Flyway. Files live in `src/main/resources/db/migration/`.
> Never modify an already-applied migration — always create a new versioned file.

---

### V1 — `categories` (seeded, not user-editable)

```sql
CREATE TABLE categories (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    icon VARCHAR(10)
);

INSERT INTO categories (name, icon) VALUES
    ('Food',          '🍔'),
    ('Transport',     '🚗'),
    ('Housing',       '🏠'),
    ('Entertainment', '🎮'),
    ('Health',        '💊'),
    ('Shopping',      '🛍️'),
    ('Travel',        '✈️'),
    ('Other',         '📦');
```

---

### V2 — `expenses`

```sql
CREATE TABLE expenses (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    amount       NUMERIC(10,2) NOT NULL,
    note         VARCHAR(255),
    category_id  INT           NOT NULL REFERENCES categories(id),
    account_id   UUID          NOT NULL REFERENCES accounts(id),   -- added V11
    expense_date DATE          NOT NULL DEFAULT CURRENT_DATE,
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expenses_date     ON expenses(expense_date);
CREATE INDEX idx_expenses_category ON expenses(category_id);
```

> `account_id` was added in V11 as a NOT NULL FK after all existing rows were
> migrated to the default account created in V10.

---

### V3 — `goals`

```sql
CREATE TYPE goal_type AS ENUM ('SHORT_TERM', 'LONG_TERM');

CREATE TABLE goals (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100)   NOT NULL,
    description   VARCHAR(255),
    target_amount NUMERIC(10,2)  NOT NULL,
    saved_amount  NUMERIC(10,2)  NOT NULL DEFAULT 0,
    goal_type     goal_type      NOT NULL,
    deadline      DATE,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    is_active     BOOLEAN        NOT NULL DEFAULT TRUE
);
```

---

### V4 — `weekly_summaries` + `llm_provider_config`

```sql
CREATE TABLE weekly_summaries (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    week_start   DATE          NOT NULL,
    week_end     DATE          NOT NULL,
    summary_text TEXT          NOT NULL,
    total_spent  NUMERIC(10,2),
    generated_at TIMESTAMP     NOT NULL DEFAULT NOW(),
    provider     VARCHAR(20)
);

CREATE TABLE llm_provider_config (
    id              INT PRIMARY KEY DEFAULT 1,
    active_provider VARCHAR(20) NOT NULL DEFAULT 'ollama',
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

INSERT INTO llm_provider_config (active_provider) VALUES ('ollama');
```

---

### V5 — `accounts`

```sql
CREATE TYPE account_type AS ENUM ('CHECKING', 'SAVINGS', 'INVESTMENT');

CREATE TABLE accounts (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                      VARCHAR(100)   NOT NULL,
    account_type              account_type   NOT NULL,
    opening_balance           NUMERIC(10,2)  NOT NULL DEFAULT 0,
    manual_balance            NUMERIC(10,2),
    manual_balance_updated_at TIMESTAMP,
    created_at                TIMESTAMP      NOT NULL DEFAULT NOW(),
    is_active                 BOOLEAN        NOT NULL DEFAULT TRUE
);
```

**Balance fields explained:**

| Field | Type | Description |
|---|---|---|
| `opening_balance` | stored | Seed value entered at account creation. Never changes. |
| `manual_balance` | stored | User-entered snapshot. Updated on demand. Nullable until first update. |
| `calculated_balance` | **derived** | `opening_balance + SUM(income) - SUM(expenses)`. Never stored. |

---

### V6 — `investment_details`

Stores investment-specific metadata. Only one row per account. Optional even for INVESTMENT accounts — only created when at least one field is provided.

```sql
CREATE TABLE investment_details (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL UNIQUE REFERENCES accounts(id),
    platform   VARCHAR(100),   -- e.g. "Revolut"
    instrument VARCHAR(100),   -- e.g. "VUAA"
    asset_type VARCHAR(50)     -- e.g. "ETF", "Stock", "Crypto"
);
```

---

### V7 — `investment_checkpoints`

Each row is a user-entered snapshot of the current market value of an investment account. Used to calculate actual return vs. contributed amount.

```sql
CREATE TABLE investment_checkpoints (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id  UUID          NOT NULL REFERENCES accounts(id),
    value       NUMERIC(10,2) NOT NULL,
    recorded_at TIMESTAMP     NOT NULL DEFAULT NOW(),
    note        VARCHAR(255)
);

CREATE INDEX idx_checkpoints_account ON investment_checkpoints(account_id);
CREATE INDEX idx_checkpoints_date    ON investment_checkpoints(recorded_at);
```

**Investment balance calculations (all derived, never stored):**

| Field | Formula |
|---|---|
| `contributed_amount` | `opening_balance + SUM(income) - SUM(expenses)` |
| `current_value` | Latest checkpoint `value`, or `contributed_amount` if no checkpoints exist |
| `return_amount` | `current_value - contributed_amount` |
| `return_percentage` | `(return_amount / contributed_amount) * 100` |

---

### V8 — `income_categories` (seeded, not user-editable)

```sql
CREATE TABLE income_categories (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    icon VARCHAR(10)
);

INSERT INTO income_categories (name, icon) VALUES
    ('Salary',    '💼'),
    ('Freelance', '💻'),
    ('Dividend',  '📈'),
    ('Gift',      '🎁'),
    ('Refund',    '↩️'),
    ('Other',     '📦');
```

---

### V9 — `income`

Mirrors the `expenses` table. Every income entry must be linked to an account.

```sql
CREATE TABLE income (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    amount             NUMERIC(10,2) NOT NULL,
    note               VARCHAR(255),
    income_category_id INT           NOT NULL REFERENCES income_categories(id),
    account_id         UUID          NOT NULL REFERENCES accounts(id),
    income_date        DATE          NOT NULL DEFAULT CURRENT_DATE,
    created_at         TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_income_date     ON income(income_date);
CREATE INDEX idx_income_category ON income(income_category_id);
CREATE INDEX idx_income_account  ON income(account_id);
```

---

### V10 — default account seed

```sql
INSERT INTO accounts (id, name, account_type, opening_balance)
VALUES ('00000000-0000-0000-0000-000000000001', 'Main Account', 'CHECKING', 0);
```

---

### V11 — add `account_id` to `expenses`

```sql
ALTER TABLE expenses ADD COLUMN account_id UUID REFERENCES accounts(id);

UPDATE expenses
SET account_id = '00000000-0000-0000-0000-000000000001'
WHERE account_id IS NULL;

ALTER TABLE expenses ALTER COLUMN account_id SET NOT NULL;
```

---

## API Endpoints

Base URL: `http://localhost:8080/api/v1`

---

### Accounts

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/accounts` | List all active accounts |
| `GET` | `/accounts/{id}` | Get account with all calculated balances |
| `POST` | `/accounts` | Create account |
| `PUT` | `/accounts/{id}` | Update account metadata |
| `DELETE` | `/accounts/{id}` | Soft delete (sets `is_active = false`) |
| `PATCH` | `/accounts/{id}/manual-balance` | Update manual balance snapshot |
| `GET` | `/accounts/{id}/checkpoints` | List all investment checkpoints |
| `POST` | `/accounts/{id}/checkpoints` | Add a new investment checkpoint |

**POST /accounts — request body:**
```json
{
  "name": "Revolut Investment",
  "accountType": "INVESTMENT",
  "openingBalance": 0.00,
  "platform": "Revolut",
  "instrument": "VUAA",
  "assetType": "ETF"
}
```

**GET /accounts/{id} — response (investment account):**
```json
{
  "id": "uuid",
  "name": "Revolut Investment",
  "accountType": "INVESTMENT",
  "openingBalance": 0.00,
  "manualBalance": 210.00,
  "manualBalanceUpdatedAt": "2025-04-01T10:00:00",
  "calculatedBalance": 200.00,
  "totalIncome": 200.00,
  "totalExpenses": 0.00,
  "contributedAmount": 200.00,
  "currentValue": 210.00,
  "returnAmount": 10.00,
  "returnPercentage": 5.00,
  "platform": "Revolut",
  "instrument": "VUAA",
  "assetType": "ETF"
}
```

**PATCH /accounts/{id}/manual-balance — request body:**
```json
{
  "manualBalance": 215.00
}
```

**POST /accounts/{id}/checkpoints — request body:**
```json
{
  "value": 210.00,
  "note": "S&P up this week"
}
```

---

### Expenses

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/expenses` | List expenses (`?month=YYYY-MM&categoryId=`) |
| `POST` | `/expenses` | Create expense |
| `PUT` | `/expenses/{id}` | Update expense |
| `DELETE` | `/expenses/{id}` | Delete expense |
| `GET` | `/expenses/summary` | Monthly totals grouped by category (`?month=`) |
| `GET` | `/expenses/stats` | Total this month, avg per day, top category |

**POST /expenses — request body:**
```json
{
  "amount": 12.50,
  "note": "Lunch at work",
  "categoryId": 1,
  "accountId": "uuid",
  "expenseDate": "2025-01-15"
}
```

> `accountId` is **required** as of Phase 2.5.

---

### Income

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/income` | List income (`?month=YYYY-MM&accountId=`) |
| `POST` | `/income` | Create income entry |
| `PUT` | `/income/{id}` | Update income entry |
| `DELETE` | `/income/{id}` | Delete income entry |
| `GET` | `/income/summary` | Monthly totals by income category (`?month=`) |
| `GET` | `/income-categories` | List all income categories |

**POST /income — request body:**
```json
{
  "amount": 1500.00,
  "note": "March salary",
  "incomeCategoryId": 1,
  "accountId": "uuid",
  "incomeDate": "2025-03-31"
}
```

---

### Goals

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/goals` | List all active goals |
| `POST` | `/goals` | Create a new goal |
| `PUT` | `/goals/{id}` | Update a goal |
| `DELETE` | `/goals/{id}` | Deactivate a goal (soft delete) |
| `POST` | `/goals/{id}/contribute` | Add amount toward a goal |
| `GET` | `/goals/{id}/progress` | Goal progress as percentage + days remaining |

**POST /goals — request body:**
```json
{
  "name": "Trip to Japan",
  "description": "Flight + hotel for 10 days",
  "targetAmount": 2500.00,
  "goalType": "LONG_TERM",
  "deadline": "2025-12-01"
}
```

---

### Categories

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/categories` | List all expense categories |
| `GET` | `/income-categories` | List all income categories |

---

### AI / Chat

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/ai/chat` | Send a message, get an AI response |
| `GET` | `/ai/summaries` | List all weekly summaries |
| `GET` | `/ai/summaries/latest` | Get the most recent weekly summary |
| `POST` | `/ai/summaries/generate` | Manually trigger a summary generation |
| `GET` | `/ai/provider` | Get the active LLM provider |
| `POST` | `/ai/provider` | Switch between `ollama` and `groq` |

**POST /ai/chat — request body:**
```json
{
  "message": "Can I afford a PS5 this month?",
  "conversationId": "optional-uuid-for-context"
}
```

**POST /ai/chat — response:**
```json
{
  "reply": "Based on your current spending, you've used €320 of your estimated €500 budget...",
  "provider": "ollama",
  "functionCallsUsed": ["getMonthlyExpenses", "getBudgetStatus"]
}
```

---

### Auth

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/register` | Register user |
| `POST` | `/auth/login` | Login, returns JWT |
| `POST` | `/auth/refresh` | Refresh JWT token |

---

## AI Integration

### Provider Strategy

Spring AI's abstraction layer makes it easy to swap providers at runtime. Both Ollama and Groq implement the same `ChatClient` interface — the router selects which one to use based on the `llm_provider_config` table.

```
User message
     │
     ▼
 AI Service
     │
     ▼
LLM Provider Router
  ├── active = 'ollama'  → OllamaChatModel (local, port 11434)
  └── active = 'groq'   → OpenAiChatModel with Groq base URL
                           (api.groq.com/openai/v1)
```

> Groq is OpenAI API-compatible, so Spring AI's `OpenAiChatModel` works with it
> out of the box — just swap the base URL and API key.

---

### Spring AI Configuration

**`application.yml`**
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: mistral
        options:
          temperature: 0.3

    openai:
      base-url: https://api.groq.com/openai/v1
      api-key: ${GROQ_API_KEY}
      chat:
        model: llama3-70b-8192
        options:
          temperature: 0.3

vault:
  ai:
    active-provider: ollama
    system-prompt: |
      You are Vault, a personal finance assistant. You have access to the
      user's real expense, income, account, and goal data through function calls.
      Always base your answers on the actual data. Be concise, practical, and
      friendly. When currency amounts are shown, use the € symbol.
```

---

### Function Calling Tools

These are Java methods annotated with `@Tool` that Spring AI automatically
makes available to the LLM during a conversation.

```java
@Component
public class FinanceTools {

    @Tool(description = "Get total expenses by category for a given month. " +
                        "Month format: YYYY-MM")
    public Map<String, Double> getExpensesByCategory(String month) {
        return expenseRepository.sumByCategoryForMonth(month);
    }

    @Tool(description = "Get the current month's total spending and " +
                        "how it compares to the previous month")
    public BudgetStatus getBudgetStatus() {
        return expenseService.getCurrentMonthStatus();
    }

    @Tool(description = "Get progress for all active goals: name, " +
                        "target, saved, percentage, and days remaining")
    public List<GoalProgress> getGoalProgress() {
        return goalService.getAllProgress();
    }

    @Tool(description = "Get daily spending for the last N days. " +
                        "Useful for trend questions.")
    public List<DailySpend> getDailySpending(int days) {
        return expenseRepository.getDailyTotals(days);
    }

    @Tool(description = "Get total spending for a specific category " +
                        "over the last N months")
    public Map<String, Double> getCategoryTrend(String category, int months) {
        return expenseRepository.getCategoryTrend(category, months);
    }

    @Tool(description = "Get all accounts with their calculated and manual balances. " +
                        "For investment accounts, includes return amount and percentage.")
    public List<AccountSummary> getAccountSummaries() {
        return accountService.getAllAccountSummaries();
    }

    @Tool(description = "Get total income by category for a given month. " +
                        "Month format: YYYY-MM")
    public Map<String, Double> getIncomeByCategory(String month) {
        return incomeRepository.sumByCategoryForMonth(month);
    }

    @Tool(description = "Get net cash flow (income minus expenses) for a given month.")
    public Double getNetCashFlow(String month) {
        return incomeService.getNetCashFlow(month);
    }
}
```

> When Phase 3 is implemented, add `getAccountSummaries`, `getIncomeByCategory`,
> and `getNetCashFlow` to `FinanceTools` so the AI can reason about accounts,
> income, and investment performance.

---

### LLM Provider Router

```java
@Service
public class LlmProviderRouter {

    private final OllamaChatModel ollamaModel;
    private final OpenAiChatModel groqModel;
    private final LlmProviderConfigRepository configRepo;

    public ChatClient getActiveClient() {
        String provider = configRepo.getActiveProvider();

        ChatModel model = switch (provider) {
            case "groq"   -> groqModel;
            default       -> ollamaModel;
        };

        return ChatClient.builder(model)
            .defaultSystem(systemPrompt)
            .defaultTools(financeTools)
            .build();
    }
}
```

---

### Weekly Summary Generation

The scheduler runs every Monday at 8:00 AM. It builds a data snapshot of
the past week and passes it to the LLM with a structured prompt.

```java
@Scheduled(cron = "0 0 8 * * MON")
public void generateWeeklySummary() {
    WeeklyDataSnapshot snapshot = buildSnapshot();

    String prompt = """
        Here is the user's financial data for the past week (%s to %s):

        Total spent: €%.2f
        Total income: €%.2f
        Net cash flow: €%.2f
        Spending by category: %s
        Income by category: %s
        Goal progress: %s
        Account balances: %s

        Write a short, friendly weekly summary (3-5 sentences). Include:
        - Where most money went
        - A comparison to last week if notable
        - One practical tip based on the data
        - Progress toward any active goals
        - Any notable investment account performance if applicable
        """.formatted(
            snapshot.weekStart(), snapshot.weekEnd(),
            snapshot.totalSpent(), snapshot.totalIncome(),
            snapshot.netCashFlow(), snapshot.byCategory(),
            snapshot.incomeByCategory(), snapshot.goals(),
            snapshot.accountSummaries()
        );

    String summary = llmProviderRouter.getActiveClient()
        .prompt(prompt)
        .call()
        .content();

    summaryRepository.save(new WeeklySummary(snapshot, summary));
}
```

---

## Implementation Phases

---

### Phase 1 — Core Data Layer ✅ Implemented

**Goal:** A working Spring Boot app with a Postgres database and REST endpoints.

**Implemented:**
- Spring Boot 4.x backend with PostgreSQL and Flyway
- Entities: `Expense`, `Category`, `Goal`, `WeeklySummary`
- Repositories with custom queries (monthly totals, category sums)
- Service layer with business logic
- REST controllers for `/expenses`, `/goals`, `/categories`, `/summaries`
- Input validation (`@Valid`, `@NotNull`, custom exception handler)
- Flyway migrations V1–V4 and category seeding

---

### Phase 2 — Next.js Frontend ✅ Implemented (basic)

**Goal:** Dashboard and UI for expense and goals tracking.

**Implemented:**
- Next.js App Router frontend
- Dashboard, expenses, and goals pages
- Typed API client (`lib/api.ts`)
- Connected to the Spring Boot backend

---

### Phase 2.5 — Accounts & Income ⬅ Current Phase

**Goal:** Add multi-account support, income tracking, and investment performance monitoring.

**Backend tasks:**
1. Flyway migrations V5–V11 (accounts, investment details, investment checkpoints, income categories, income, account FK on expenses)
2. New entities: `Account`, `InvestmentDetail`, `InvestmentCheckpoint`, `IncomeCategory`, `Income`
3. Update `Expense` entity with `account` FK
4. New repositories with balance sum queries
5. `AccountService` with derived balance calculations (calculated, manual, investment returns)
6. `InvestmentCheckpointService`
7. `IncomeService` + `IncomeCategoryService`
8. New controllers: `AccountController`, `IncomeController`, `IncomeCategoryController`
9. Update `ExpenseService` / `ExpenseController` to require `accountId`

**Frontend tasks:**
1. New TypeScript types for Account, Income, Checkpoint
2. New API client functions
3. Accounts list page with balance cards and weekly update nudge
4. Create/edit account form (with conditional investment fields)
5. Manual balance update modal
6. Investment account detail page with checkpoint history and performance chart
7. Income list page (mirrors expenses page)
8. Income form (mirrors expense form)
9. Update expense form to require account selection
10. Dashboard: accounts summary strip, net worth figure, stale balance nudge

**Key design decisions:**
- `calculated_balance` is always derived at query time — never stored
- `manual_balance` is a user-entered snapshot, updated on demand with a weekly prompt
- Investment accounts have an additional `current_value` from the latest checkpoint, enabling return % calculation
- No transfers between accounts in this phase — income and expenses are always external money in/out
- All monetary values use `BigDecimal` (never `double`)
- Soft delete on accounts (`is_active = false`)

**Deliverable:** Full account management, income tracking, and investment performance monitoring alongside the existing expense and goals features.

---

### Phase 3 — AI Integration

**Goal:** Connect the app to Ollama locally and Groq as a cloud option,
with function calling so the LLM can query real data.

**Tasks:**
1. Add Spring AI dependencies: `spring-ai-ollama-spring-boot-starter`, `spring-ai-openai-spring-boot-starter`
2. Configure both providers in `application.yml`
3. Implement `FinanceTools` with all `@Tool` methods — including the new account, income, and net cash flow tools added in Phase 2.5
4. Implement `LlmProviderRouter`
5. Implement `/ai/chat` with conversation context support
6. Implement `/ai/provider` (GET + POST) for runtime switching
7. Build chat UI in Next.js — message thread + input box
8. Add provider toggle badge (shows "Ollama" or "Groq", clickable to switch)
9. Test with real questions: "How much did I spend on food?", "What's my net worth?", "How is my Revolut investment performing?"

**Deliverable:** A working chat interface that reasons over expenses, income, accounts, and goals.

---

### Phase 4 — Weekly Summary Automation

**Goal:** Automated weekly AI-generated reports that appear on the dashboard.

**Tasks:**
1. Implement `WeeklyDataSnapshot` builder — now includes income, net cash flow, and account summaries alongside existing expense and goal data
2. Implement the `@Scheduled` job with the updated structured prompt
3. Implement manual trigger `POST /ai/summaries/generate`
4. Save summaries to `weekly_summaries` with provider recorded
5. Build the summary card on the dashboard
6. Build the summary history page

**Deliverable:** Every Monday morning a fresh summary covering spending, income, net cash flow, and investment performance is waiting on the dashboard.

---

### Phase 5 — Auth & Polish

**Goal:** Secure the app and add quality-of-life improvements.

**Tasks:**
1. Add Spring Security with JWT — register, login, refresh token
2. Secure all API routes (public: `/auth/**`, protected: everything else)
3. Add JWT handling to the Next.js frontend
4. Add expense and income bulk delete
5. Add goal contribution history
6. Add transfer support between accounts (moves money from one account to another atomically)
7. Add a smart alert system:
   - "You've spent 80% of last month's average on Food with 2 weeks to go"
   - "You're behind on your Japan trip goal — you need €X more per month"
   - "Your Revolut investment is up 12% — consider logging a checkpoint"
8. Add Swagger UI / OpenAPI docs (`springdoc-openapi`)
9. Dockerize: `Dockerfile` for Spring Boot, `docker-compose.yml` with Postgres + the app

**Deliverable:** A production-ready, secured, fully documented app.

---

## Suggested Models

| Provider | Model | Notes |
|---|---|---|
| Ollama | `mistral` | Fast, good for function calling, runs on 8GB VRAM |
| Ollama | `llama3` | Better reasoning, needs 16GB VRAM |
| Groq | `llama3-70b-8192` | Free tier, very fast inference, best quality |
| Groq | `mixtral-8x7b-32768` | Good alternative on Groq free tier |

> **Recommended setup:** Use Ollama locally for daily use (private, free,
> no latency). Switch to Groq when you want better reasoning quality or
> if Ollama is slow on your hardware.

---

## Project Structure

```
vault/
├── backend/                              # Spring Boot
│   ├── src/main/java/com/vfa/vault/
│   │   ├── config/                       # Security, Spring AI beans
│   │   ├── controller/
│   │   │   ├── AccountController.java
│   │   │   ├── CategoryController.java
│   │   │   ├── ExpenseController.java
│   │   │   ├── GoalController.java
│   │   │   ├── IncomeCategoryController.java
│   │   │   ├── IncomeController.java
│   │   │   └── WeeklySummaryController.java
│   │   ├── service/
│   │   │   ├── AccountService.java
│   │   │   ├── CategoryService.java
│   │   │   ├── ExpenseService.java
│   │   │   ├── GoalService.java
│   │   │   ├── IncomeCategoryService.java
│   │   │   ├── IncomeService.java
│   │   │   ├── InvestmentCheckpointService.java
│   │   │   └── WeeklySummaryService.java
│   │   ├── repository/
│   │   │   ├── AccountRepository.java
│   │   │   ├── CategoryRepository.java
│   │   │   ├── ExpenseRepository.java
│   │   │   ├── GoalRepository.java
│   │   │   ├── IncomeCategoryRepository.java
│   │   │   ├── IncomeRepository.java
│   │   │   ├── InvestmentCheckpointRepository.java
│   │   │   ├── InvestmentDetailRepository.java
│   │   │   └── WeeklySummaryRepository.java
│   │   ├── entity/
│   │   │   ├── Account.java
│   │   │   ├── Category.java
│   │   │   ├── Expense.java
│   │   │   ├── Goal.java
│   │   │   ├── Income.java
│   │   │   ├── IncomeCategory.java
│   │   │   ├── InvestmentCheckpoint.java
│   │   │   ├── InvestmentDetail.java
│   │   │   └── WeeklySummary.java
│   │   ├── dto/
│   │   │   ├── AccountDTO.java
│   │   │   ├── AccountResponseDTO.java
│   │   │   ├── CategoryDTO.java
│   │   │   ├── ExpenseDTO.java
│   │   │   ├── GoalDTO.java
│   │   │   ├── IncomeCategoryDTO.java
│   │   │   ├── IncomeDTO.java
│   │   │   ├── IncomeResponseDTO.java
│   │   │   ├── InvestmentCheckpointDTO.java
│   │   │   ├── InvestmentCheckpointResponseDTO.java
│   │   │   └── ManualBalanceDTO.java
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── ResourceNotFoundException.java
│   │   ├── ai/                           # Phase 3+
│   │   │   ├── FinanceTools.java
│   │   │   └── LlmProviderRouter.java
│   │   └── scheduler/                    # Phase 4+
│   │       └── WeeklySummaryScheduler.java
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   └── db/migration/
│   │       ├── V1__create_categories.sql
│   │       ├── V2__create_expenses.sql
│   │       ├── V3__create_goals.sql
│   │       ├── V4__create_summaries_and_config.sql
│   │       ├── V5__create_accounts.sql
│   │       ├── V6__create_investment_details.sql
│   │       ├── V7__create_investment_checkpoints.sql
│   │       ├── V8__create_income_categories.sql
│   │       ├── V9__create_income.sql
│   │       ├── V10__add_default_account.sql
│   │       └── V11__add_account_to_expenses.sql
│   └── pom.xml
│
└── frontend/                             # Next.js
    ├── app/
    │   ├── accounts/
    │   │   ├── page.tsx                  # accounts list
    │   │   └── [id]/page.tsx             # investment detail
    │   ├── dashboard/
    │   ├── expenses/
    │   ├── goals/
    │   ├── income/
    │   │   └── page.tsx
    │   └── chat/                         # Phase 3+
    ├── components/
    │   ├── accounts/
    │   │   ├── AccountForm.tsx
    │   │   └── ManualBalanceModal.tsx
    │   └── income/
    │       └── IncomeForm.tsx
    ├── lib/
    │   ├── api.ts                        # typed API client
    │   └── types.ts                      # shared TypeScript types
    └── package.json
```