# Vault API — Personal Finance Backend

A **Spring Boot 4.0** REST API for personal finance tracking with password-protected access, AI-powered insights, and multi-account support. Built for privacy-first finance management with optional LLM integration (LM Studio + Groq).

## Table of Contents

1. [What is Vault?](#what-is-vault)
2. [Key Features](#key-features)
3. [Getting Started](#getting-started)
4. [How It Works](#how-it-works)
5. [Project Structure](#project-structure)
6. [API Overview](#api-overview)
7. [Security Architecture](#security-architecture)
8. [Development](#development)
9. [Deployment](#deployment)

---

## What is Vault?

Vault is a **single-user personal finance API** designed for maximum privacy and simplicity:

- **No user registration** – one shared password protects all data
- **No cloud AI dependencies** – chat with your own financial data using local LLMs (optional)
- **Live balance calculations** – account balances update in real-time based on transactions
- **Multi-account support** – organize spending across Checking, Savings, and Investment accounts
- **AI-powered insights** – ask questions about your finances and get intelligent responses backed by your actual spending data
- **Self-hosted friendly** – runs locally or on any cloud platform with a PostgreSQL database

### Use Cases

- **Personal finance tracking** – categorized expenses and income linked to accounts
- **Investment monitoring** – track multiple investment accounts with checkpoint-based return calculations
- **Goal planning** – create savings goals and track progress toward milestones
- **Financial insights** – ask your AI assistant questions like "Can I afford a vacation?" and get answers based on your real data
- **Weekly summaries** – automated AI-generated weekly financial reports (every Monday at 8am)

---

## Key Features

### 💰 Core Finance Features

- **Multi-Account Support**
  - **Checking/Savings accounts** – for everyday spending and savings
  - **Investment accounts** – track investments with optional platform/instrument metadata
  - **Live balance calculation** – `Balance = Opening Balance + Income - Expenses` (calculated on demand, never stored)

- **Expense Tracking**
  - Categorized expenses (Food, Transport, Entertainment, etc.)
  - Linked to a specific account
  - Date-based filtering and monthly summaries
  - Dashboard stats: total this month, average per day, top spending category

- **Income Tracking**
  - Multiple income categories (Salary, Freelance, Dividend, Gift, Refund, Other)
  - Linked to accounts
  - Monthly summaries by category

- **Investment Checkpoints**
  - Record market value snapshots of investment accounts
  - Automatically calculate: contributed amount, current value, return amount, return %
  - Track performance over time without storing the actual balance

- **Financial Goals**
  - Create short/long-term goals with target amounts
  - Track contributions and progress toward each goal
  - Soft delete (deactivate) instead of hard delete

### 🤖 AI & Insights

- **Conversational Finance Chat**
  - Ask questions about your spending, goals, and accounts in natural language
  - Backend uses **function-calling** — the LLM can access tools to query your real financial data
  - Powered by either **LM Studio** (local, private) or **Groq API** (cloud, free tier available)
  - Optional conversation memory for context-aware follow-ups

- **Automated Weekly Summaries**
  - Scheduled every Monday at 8am
  - AI-generated analysis covering: spending trends, net cash flow, income sources, goal progress, investment returns
  - Stored with provider/model metadata for audit trail
  - Manual trigger available via API

- **Provider & Model Selection**
  - Choose different LLM providers for chat vs. summaries
  - Change models on the fly (e.g., use Mistral 7B for fast chat, Llama3 70B for detailed summaries)
  - Live model discovery from both LM Studio and Groq

### 🔐 Security & Access Control

- **Password-Gate Authentication**
  - Single shared vault password (no user accounts)
  - Minimum 8 characters, hashed with BCrypt + automatic salt
  - HTTP-only cookies prevent XSS attacks
  - JWT tokens with 24-hour expiry

- **Rate Limiting**
  - Protects `/auth/setup` and `/auth/login`
  - 5 attempts per 15 minutes per IP address
  - Proxy-aware (detects X-Forwarded-For headers for cloud deployments)

- **CORS Support**
  - Configured for cross-origin requests (critical for Render backend + Vercel frontend)
  - Credentials-enabled (cookies included in cross-origin requests)

---

## Getting Started

### Prerequisites

- **Java 21** – Download from [oracle.com](https://www.oracle.com/java/technologies/downloads/#java21)
- **PostgreSQL 12+** – Either local installation or remote database (e.g., Supabase)
- **Git** – For cloning the repository
- *(Optional)* **LM Studio** – For local LLM inference ([lmstudio.ai](https://lmstudio.ai)) or **Groq API** for cloud LLMs

### Step 1: Clone & Setup

```bash
# Clone the repository
git clone <repository-url>
cd vault-api

# Create a .env file in the project root
# Linux/Mac:
touch .env
# Windows (PowerShell):
ni .env
```

### Step 2: Configure Environment Variables

Edit `.env` and add the following (use the examples as a starting point):

```properties
# ========== DATABASE ==========
DB_PASSWORD=your_postgres_password
# If using Supabase, the full connection string is configured in application.yaml

# ========== JWT & AUTH (REQUIRED) ==========
VAULT_JWT_SECRET=generate-random-32-chars-here
# Generate with: openssl rand -base64 32
# Minimum 32 characters. This signs all JWT tokens.

# ========== COOKIE SETTINGS ==========
# For local dev:
VAULT_COOKIE_SECURE=false
VAULT_COOKIE_SAME_SITE=Strict

# For production (Render + Vercel):
VAULT_COOKIE_SECURE=true
VAULT_COOKIE_SAME_SITE=None

# ========== CORS ==========
# Must match your frontend domain exactly
FRONTEND_URL=http://localhost:3000
# Production: https://your-frontend.vercel.app

# ========== AI PROVIDERS (OPTIONAL) ==========
# For LM Studio (local):
# No key needed — LM Studio accepts any non-empty string
# Just ensure LM Studio is running on http://localhost:1234

# For Groq (cloud, free tier):
GROQ_API_KEY=your-groq-api-key-from-groq.com
```

**Important Notes:**
- `VAULT_JWT_SECRET` must be **at least 32 random characters**. Use `openssl rand -base64 32` to generate a secure value.
- `.env` is loaded automatically by Spring Boot via `spring.config.import=file:.env[.optional]` in `application.yaml`.
- Never commit `.env` to Git (it's in `.gitignore`).

### Step 3: Initialize the Database

The database is set up automatically on first run using **Flyway migrations**. You only need a blank PostgreSQL database:

```bash
# Using psql (PostgreSQL CLI):
createdb vault_db
createuser vault_user --password
# Enter your chosen password

# Or use Supabase:
# 1. Sign up at https://supabase.com
# 2. Create a new project and database
# 3. Copy the connection string into application.yaml
```

**Database connection** is configured in `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/vault_db
    username: vault_user
    password: ${DB_PASSWORD}
```

### Step 4: Run the Application

```powershell
# Using Maven Wrapper (PowerShell on Windows):
.\mvnw.cmd spring-boot:run

# Or on Linux/Mac:
./mvnw spring-boot:run
```

You'll see output like:
```
Started VaultApplication in 3.2 seconds
Tomcat started on port(s): 8080 (http)
```

**Server is ready at:** `http://localhost:8080`

### Step 5: Verify Setup

The vault starts in an **unconfigured state**. Check the API:

```bash
curl http://localhost:8080/api/v1/auth/status
```

Response:
```json
{ "configured": false }
```

Then, use your frontend (or curl) to set up the vault password:

```bash
curl -X POST http://localhost:8080/api/v1/auth/setup \
  -H "Content-Type: application/json" \
  -d '{"password": "my-vault-password"}' \
  -c cookies.txt
```

Response:
```json
{ "message": "Vault configured successfully" }
```

A JWT token is now stored in your browser's HTTP-only cookie (or in `cookies.txt` if using curl). All subsequent requests include this token automatically.

---

## How It Works: Backend Architecture

### High-Level Flow

```
┌─────────────────────────────────────────────────────────┐
│              Frontend (Next.js)                         │
│  1. Check: GET /auth/status → is vault configured?     │
│  2. Setup: POST /auth/setup (first time only)          │
│  3. Auth: POST /auth/login (subsequent times)          │
│  4. All requests auto-include JWT from cookie          │
└──────────────────┬──────────────────────────────────────┘
                   │ HTTPS / REST
┌──────────────────▼──────────────────────────────────────┐
│        Spring Boot Backend (Java 21)                    │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Security Layer                                  │   │
│  │  • CORS Filter (allow frontend domain)          │   │
│  │  • Rate Limit Filter (5/15min per IP)           │   │
│  │  • JWT Filter (extract & validate token)        │   │
│  └──────────────────────────────────────────────────┘   │
│                       ↓                                  │
│  ┌──────────────────────────────────────────────────┐   │
│  │  REST Controllers                               │   │
│  │  • /auth/*  (public: setup, login, refresh)    │   │
│  │  • /expenses, /income, /accounts (protected)    │   │
│  │  • /ai/* (chat, summaries, config)              │   │
│  └──────────────────────────────────────────────────┘   │
│                       ↓                                  │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Service Layer                                   │   │
│  │  • Business logic & data validation             │   │
│  │  • Transaction management                       │   │
│  │  • Account balance calculations                 │   │
│  └──────────────────────────────────────────────────┘   │
│                       ↓                                  │
│  ┌──────────────────────────────────────────────────┐   │
│  │  LLM Provider Router (AI)                        │   │
│  │  • Choose LM Studio or Groq for chat            │   │
│  │  • Choose different provider for summaries      │   │
│  │  • Pass financial tools for function-calling    │   │
│  └──────────────────────────────────────────────────┘   │
│                       ↓                                  │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Data Access Layer (JPA/Hibernate)              │   │
│  │  • Automatically mapped SQL ↔ Java objects      │   │
│  │  • No manual SQL queries                        │   │
│  └──────────────────────────────────────────────────┘   │
└──────────────────┬──────────────────────────────────────┘
                   │ JDBC
┌──────────────────▼──────────────────────────────────────┐
│         PostgreSQL Database                             │
│                                                          │
│  14 Flyway Migrations (auto-applied on startup)         │
│  • accounts, expenses, income, goals, summaries         │
│  • investments, checkpoints, categories                 │
│  • app_config (stores vault password hash)              │
└──────────────────────────────────────────────────────────┘
```

### Authentication Flow (Password-Gate Model)

Unlike traditional multi-user apps, Vault uses a **single shared password**:

```
First Time Setup:
  1. User opens frontend → GET /auth/status → { configured: false }
  2. Frontend shows setup form
  3. User enters password → POST /auth/setup { password: "my-password" }
  4. Backend:
     a. Hash password with BCrypt (automatic salt generation)
     b. Store hash in app_config table (not stored as plain text)
     c. Generate JWT token signed with VAULT_JWT_SECRET
     d. Return JWT in Set-Cookie header (HttpOnly flag, path=/api/v1)
  5. Browser receives JWT in HttpOnly cookie, can't be accessed by JavaScript (XSS protection)

Subsequent Access:
  1. User → POST /auth/login { password: "my-password" }
  2. Backend:
     a. Retrieve hashed password from app_config
     b. Compare user's password against hash using BCrypt.matches()
     c. If match, issue new JWT token
  3. All future requests automatically include the cookie
  4. JwtFilter intercepts requests, extracts JWT, validates signature & expiry
  5. If valid, populate Spring Security context; if expired, return 401

Logout:
  1. POST /auth/logout
  2. Backend returns Set-Cookie with maxAge=0 (deletes cookie)
```

### Why This Architecture?

| Aspect | Benefit |
|--------|---------|
| **Single password** | No user management, simpler for personal finance |
| **HttpOnly cookies** | Immune to XSS (JavaScript can't read the token) |
| **JWT tokens** | Stateless auth (no session database needed), 24-hour expiry |
| **BCrypt hashing** | Slow by design, resists brute-force attacks |
| **Rate limiting** | Blocks automated password guessing (5 attempts/15 min/IP) |

---

## Project Structure

```
vault-api/
├── src/main/java/com/vfa/vault/
│   ├── VaultApplication.java              # Spring Boot entry point
│   ├── WebConfig.java                     # CORS configuration
│   │
│   ├── config/                            # Security & beans
│   │   ├── SecurityConfig.java            # Spring Security filter chain
│   │   ├── JwtUtil.java                   # Generate & validate JWT
│   │   ├── JwtFilter.java                 # Extract token from cookie
│   │   ├── RateLimitFilter.java           # 5 attempts/15min per IP
│   │   ├── CookieUtil.java                # Build HttpOnly cookies
│   │   └── AiConfig.java                  # Define LM Studio + Groq beans
│   │
│   ├── controller/                        # REST API endpoints
│   │   ├── AuthController.java            # /api/v1/auth/*
│   │   ├── AccountController.java         # /api/v1/accounts/*
│   │   ├── ExpenseController.java         # /api/v1/expenses/*
│   │   ├── IncomeController.java          # /api/v1/income/*
│   │   ├── GoalController.java            # /api/v1/goals/*
│   │   ├── CategoryController.java        # /api/v1/categories
│   │   └── AiController.java              # /api/v1/ai/*
│   │
│   ├── service/                           # Business logic
│   │   ├── AccountService.java
│   │   ├── ExpenseService.java
│   │   ├── IncomeService.java
│   │   ├── GoalService.java
│   │   └── WeeklySummaryService.java
│   │
│   ├── ai/                                # AI integration
│   │   ├── LlmProviderRouter.java         # Choose provider/model per task
│   │   ├── FinanceTools.java              # @Tool methods for LLM
│   │   └── ModelDiscoveryService.java     # Fetch available models
│   │
│   ├── repository/                        # Data access (JPA)
│   │   ├── AccountRepository.java
│   │   ├── ExpenseRepository.java
│   │   ├── IncomeRepository.java
│   │   └── ... (8 more repositories)
│   │
│   ├── entity/                            # Database models
│   │   ├── Account.java
│   │   ├── Expense.java
│   │   ├── Income.java
│   │   ├── Goal.java
│   │   ├── InvestmentCheckpoint.java
│   │   └── ... (5 more entities)
│   │
│   ├── dto/                               # Request/Response DTOs
│   │   ├── ExpenseDTO.java
│   │   ├── AccountDTO.java
│   │   ├── ChatRequestDTO.java
│   │   └── ... (9 more DTOs)
│   │
│   ├── exception/                         # Error handling
│   │   ├── GlobalExceptionHandler.java
│   │   └── ResourceNotFoundException.java
│   │
│   └── scheduler/                         # Background jobs
│       └── WeeklySummaryScheduler.java    # Runs every Monday 8am
│
├── src/main/resources/
│   ├── application.yaml                   # Spring Boot config
│   └── db/migration/                      # Flyway migrations (14 versions)
│       ├── V1__create_categories.sql
│       ├── V2__create_expenses.sql
│       ├── ...
│       └── V14__create_app_config.sql
│
├── pom.xml                                # Maven dependencies
├── .env.example                           # Template for environment variables
└── README.md                              # This file
```

### Database Schema (14 Flyway Migrations)

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `categories` | Expense categories (seeded) | id, name, icon |
| `expenses` | Financial transactions | id, amount, category_id, account_id, expense_date |
| `accounts` | Bank/investment accounts | id, name, account_type, opening_balance |
| `investment_details` | Optional investment metadata | account_id, platform, instrument, asset_type |
| `investment_checkpoints` | Market value snapshots | account_id, value, recorded_at |
| `income_categories` | Income categories (seeded) | id, name, icon |
| `income` | Income entries | id, amount, income_category_id, account_id, income_date |
| `goals` | Financial savings goals | id, name, target_amount, saved_amount, deadline |
| `weekly_summaries` | AI-generated summaries | id, week_start, summary_text, provider, model |
| `llm_provider_config` | AI provider settings | id, chat_provider, chat_model, summary_provider, summary_model |
| `app_config` | Vault configuration | key (e.g., "vault_password_hash"), value |

**Note:** All migrations run automatically on application startup via Flyway. Data is never hard-deleted; entities use soft deletes (`is_active` flag).

---

## API Overview

**Base URL:** `http://localhost:8080/api/v1`

All endpoints except `/auth/status`, `/auth/setup`, and `/auth/login` require a valid JWT in the `vault_token` HTTP-only cookie.

### Authentication Endpoints

| Method | Endpoint | Description | Rate Limited |
|--------|----------|-------------|--------------|
| GET | `/auth/status` | Check if vault is configured | No |
| POST | `/auth/setup` | Initialize vault with password (first-time only) | Yes* |
| POST | `/auth/login` | Authenticate with vault password | Yes* |
| GET | `/auth/verify` | Verify JWT is valid (heartbeat) | No |
| POST | `/auth/refresh` | Issue a new 24-hour JWT token | No |
| POST | `/auth/logout` | Clear authentication cookie | No |

*5 attempts per 15 minutes per IP address

#### Example: Setup & Login

```bash
# 1. Check status (no auth required)
curl http://localhost:8080/api/v1/auth/status
→ { "configured": false }

# 2. Setup (first time)
curl -X POST http://localhost:8080/api/v1/auth/setup \
  -H "Content-Type: application/json" \
  -d '{"password": "my-secure-password"}' \
  -c cookies.txt
→ { "message": "Vault configured successfully" }

# 3. Login (subsequent times)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"password": "my-secure-password"}' \
  -c cookies.txt
→ { "message": "Login successful" }

# 4. Use the vault (automatic cookie inclusion)
curl -b cookies.txt http://localhost:8080/api/v1/accounts
→ [{ "id": "...", "name": "Main Account", ... }]
```

### Accounts

Organize money across multiple account types:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/accounts` | List all active accounts with live calculated balances |
| POST | `/accounts` | Create new account (CHECKING, SAVINGS, or INVESTMENT) |
| PUT | `/accounts/{id}` | Update account metadata |
| DELETE | `/accounts/{id}` | Soft delete (deactivate) account |
| PATCH | `/accounts/{id}/manual-balance` | Set a manual balance snapshot |
| GET | `/accounts/{id}/checkpoints` | List investment checkpoints (INVESTMENT only) |
| POST | `/accounts/{id}/checkpoints` | Record investment market value |

**Example: Create Account**
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Investment Brokerage",
    "accountType": "INVESTMENT",
    "openingBalance": 5000.00,
    "platform": "Fidelity",
    "instrument": "ETF Portfolio",
    "assetType": "Mixed ETFs"
  }' \
  -b cookies.txt
```

### Expenses

Track spending by category:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/expenses` | List expenses (filter by month or category) |
| POST | `/expenses` | Create expense entry |
| PUT | `/expenses/{id}` | Update expense |
| DELETE | `/expenses/{id}` | Delete expense |
| GET | `/expenses/summary` | Monthly totals by category |
| GET | `/expenses/stats` | Dashboard stats (total, avg per day, top category) |

### Income

Track money coming in:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/income` | List income entries |
| POST | `/income` | Create income entry |
| PUT | `/income/{id}` | Update income |
| DELETE | `/income/{id}` | Delete income |
| GET | `/income/summary` | Monthly totals by category |
| GET | `/income-categories` | List all income categories |

### Goals

Set and track financial targets:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/goals` | List active goals |
| POST | `/goals` | Create new goal |
| PUT | `/goals/{id}` | Update goal |
| DELETE | `/goals/{id}` | Deactivate goal |
| POST | `/goals/{id}/contribute` | Add money toward goal |
| GET | `/goals/{id}/progress` | Get progress percentage & days remaining |

### AI & Chat

Talk to an LLM about your finances:

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/ai/chat` | Send message, get AI response with financial data access |
| GET | `/ai/config` | Get current provider/model configuration |
| PATCH | `/ai/config` | Change provider or model (for chat or summaries) |
| GET | `/ai/models/lmstudio` | Discover models available in LM Studio |
| GET | `/ai/models/groq` | Discover models available in Groq API |
| GET | `/ai/summaries` | List all weekly summaries |
| GET | `/ai/summaries/latest` | Get most recent summary |
| POST | `/ai/summaries/generate` | Manually trigger summary generation |

**Example: Chat with AI**
```bash
curl -X POST http://localhost:8080/api/v1/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "How much have I spent on food this month?",
    "conversationId": "optional-uuid-for-memory"
  }' \
  -b cookies.txt
→ {
  "reply": "Based on your transactions, you spent €245 on food this month...",
  "provider": "lmstudio",
  "model": "mistral-7b-instruct"
}
```

---

## Security Architecture

### Password Storage

1. User enters password → Hashed with **BCrypt** (slow, salted) → Stored in `app_config` table
2. On login, user's input is hashed again and compared against stored hash
3. Plain text password is never stored or logged

### JWT Tokens

- **Signing algorithm:** HMAC SHA-256
- **Secret:** `VAULT_JWT_SECRET` (must be 32+ characters)
- **Expiry:** 24 hours
- **Storage:** HTTP-only cookie (not accessible to JavaScript)
- **Validation:** Signature verified on every request; expired tokens rejected with 401

### Cookie Security

| Setting | Local Dev | Production | Purpose |
|---------|-----------|------------|---------|
| `Secure` | false | true | Only sent over HTTPS (prevents man-in-the-middle) |
| `HttpOnly` | true | true | Not accessible to JavaScript (prevents XSS theft) |
| `SameSite` | Strict | None | Cross-origin policy (None for Render + Vercel) |

### Rate Limiting

- **Protected endpoints:** `/auth/setup`, `/auth/login`
- **Limit:** 5 attempts per 15 minutes per IP address
- **Implementation:** Bucket4j token bucket algorithm
- **IP detection:** Proxy-aware (checks `X-Forwarded-For` and `X-Real-IP` headers)

### CORS Configuration

- Allows requests from `FRONTEND_URL` only
- Includes credentials (cookies) in cross-origin requests
- Other origins are rejected at the browser level

---

## Development

### Build & Compile

```powershell
# Compile without running tests
.\mvnw.cmd compile

# Run all unit tests
.\mvnw.cmd test

# Run specific test class
.\mvnw.cmd test -Dtest=ExpenseServiceTest
```

### Running Tests

```bash
# Run all tests (verbose output)
.\mvnw.cmd test -X

# Run with test code coverage
.\mvnw.cmd test jacoco:report
```

### Configuration Files

- **[application.yaml](src/main/resources/application.yaml)** – Spring Boot configuration
  - Database connection
  - Flyway migration settings
  - JWT and cookie configuration
  - AI provider base URLs

- **[pom.xml](pom.xml)** – Maven dependencies
  - Spring Boot, Spring AI, Spring Security
  - Bucket4j (rate limiting), JJWT (JWT), BCrypt
  - PostgreSQL driver, Flyway

### Common Development Tasks

#### Add a New Endpoint

1. Create a `@RestController` in `src/main/java/com/vfa/vault/controller/`
2. Inject required services via `@Autowired` or constructor injection
3. Decorate method with `@GetMapping`, `@PostMapping`, etc.
4. Protected endpoints are automatically checked by Spring Security

#### Modify Database Schema

1. Create a new migration file: `src/main/resources/db/migration/V{N}__{description}.sql`
2. Always use `CREATE TABLE IF NOT EXISTS` and `ALTER TABLE IF EXISTS` (idempotence)
3. On next startup, Flyway automatically applies the migration
4. **Never modify applied migrations** — always create new versions

#### Add AI Function-Calling Tool

1. Add a `@Tool` method to `[FinanceTools.java](src/main/java/com/vfa/vault/ai/FinanceTools.java)`
2. Methods are automatically available to the LLM via function calling
3. Return typed data (String, List, DTO) — Spring AI serializes to JSON
4. Restart the application for changes to take effect

### IDE Setup (VS Code + Extension Pack for Java)

1. Install [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)
2. Open the vault-api folder
3. VS Code automatically detects pom.xml and sets up the project
4. Maven dependencies auto-download on first build
5. F5 to debug, Ctrl+Alt+U to see class hierarchy

---

## Deployment

### Local Development

```powershell
# Start the application
.\mvnw.cmd spring-boot:run

# Server runs on http://localhost:8080
# Database must be accessible (local PostgreSQL or Supabase)
```

### Cloud Deployment (Render + Vercel)

#### Backend (Render.com)

1. Sign up at [render.com](https://render.com)
2. Create a **Web Service** and connect your GitHub repository
3. Set build command: `mvn clean package -DskipTests`
4. Set start command: `java -jar target/vault-api.jar`
5. Add environment variables:
   ```
   VAULT_JWT_SECRET=<random-32-char-string>
   VAULT_COOKIE_SECURE=true
   VAULT_COOKIE_SAME_SITE=None
   FRONTEND_URL=<your-vercel-domain>
   DB_PASSWORD=<postgres-password>
   GROQ_API_KEY=<your-groq-key-if-using>
   ```
6. Set up PostgreSQL database (Supabase recommended):
   - Sign up at [supabase.com](https://supabase.com)
   - Create a new project and copy the connection string
   - Update `application.yaml` datasource URL
7. Deploy — Render detects Maven and auto-builds/deploys on push

#### Frontend (Vercel)

1. Deploy your Next.js frontend to [vercel.com](https://vercel.com)
2. Set environment variable: `NEXT_PUBLIC_API_URL=<your-render-backend-domain>`
3. Deploy — Vercel auto-detects Next.js, builds, and deploys

#### Database (Supabase)

1. Sign up at [supabase.com](https://supabase.com)
2. Create a project and get the PostgreSQL connection string
3. Update connection string in `application.yaml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://db.supabase.co:5432/postgres
       username: postgres
       password: ${DB_PASSWORD}
   ```
4. Flyway migrations run automatically on first deploy

#### Deployment Checklist

- [ ] `VAULT_JWT_SECRET` is 32+ random characters
- [ ] `VAULT_COOKIE_SECURE=true` in production
- [ ] `VAULT_COOKIE_SAME_SITE=None` for cross-origin
- [ ] `FRONTEND_URL` matches your Vercel domain exactly
- [ ] Database is set up and accessible from Render
- [ ] All environment variables are configured in Render dashboard
- [ ] HTTPS is enabled (both Render and Vercel enforce this)
- [ ] Email notifications enabled for deployment failures
- [ ] Database backups are enabled (Supabase settings)

---

## Tech Stack Summary

| Layer | Technology |
|-------|-----------|
| **Runtime** | Java 21 |
| **Framework** | Spring Boot 4.0.6 |
| **Web Framework** | Spring Web MVC |
| **Database** | PostgreSQL 12+ |
| **Migrations** | Flyway 11.14.1 |
| **ORM** | Spring Data JPA / Hibernate |
| **Authentication** | JWT (JJWT 0.12.6) + BCrypt |
| **Rate Limiting** | Bucket4j 8.10.1 |
| **Security** | Spring Security 7.0.5 |
| **AI Integration** | Spring AI (OpenAI-compatible) |
| **Build System** | Maven Wrapper |
| **Data Validation** | Spring Validation (JSR-303) |

---

## Validation Rules & Constraints

### Account

- `name` – Required, max 100 characters
- `accountType` – Required, enum: `CHECKING`, `SAVINGS`, `INVESTMENT`
- `openingBalance` – ≥ 0
- `manualBalance` – ≥ 0, optional override

### Expense

- `amount` – Required, > 0
- `note` – Optional, max 255 characters
- `accountId` – Required (must be active)
- `categoryId` – Required (must exist)
- `expenseDate` – Optional, defaults to today

### Income

- `amount` – Required, > 0
- `note` – Optional, max 255 characters
- `accountId` – Required (must be active)
- `incomeCategoryId` – Required (must exist)
- `incomeDate` – Optional, defaults to today

### Goal

- `name` – Required, max 100 characters
- `targetAmount` – Required, > 0
- `contribution` – When adding, must be > 0 and ≤ remaining target
- `deadline` – Optional, must be future date if provided

### Investment Checkpoint

- `value` – Required, > 0
- `note` – Optional, max 255 characters

---

## Error Handling

All errors follow a consistent JSON format:

### Standard Error Response

```json
{
  "status": 404,
  "message": "Account not found with id: 123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "2026-05-04T10:30:00Z"
}
```

### Validation Error Response

```json
{
  "amount": "Amount must be greater than 0",
  "accountId": "Account is required"
}
```

### HTTP Status Codes

| Status | Description |
|--------|-------------|
| `200` | OK — request succeeded |
| `201` | Created — resource created successfully |
| `400` | Bad Request — validation failure or invalid operation |
| `401` | Unauthorized — JWT missing, invalid, or expired |
| `403` | Forbidden — authenticated but not allowed |
| `404` | Not Found — resource does not exist |
| `429` | Too Many Requests — rate limit exceeded (5/15min per IP) |
| `500` | Internal Server Error — unexpected error |

---

## Balance Calculations

Account balances are **never stored**—always calculated on demand for accuracy:

$$\text{Account Balance} = \text{Opening Balance} + \sum(\text{Income}) - \sum(\text{Expenses})$$

### Investment Accounts (Special Fields)

For `INVESTMENT` type accounts, the following additional fields are calculated:

| Field | Formula |
|-------|---------|
| `contributedAmount` | Same as Account Balance (opening + income - expenses) |
| `currentValue` | Latest checkpoint value, or `contributedAmount` if no checkpoints exist |
| `returnAmount` | `currentValue - contributedAmount` |
| `returnPercentage` | $\frac{\text{returnAmount}}{\text{contributedAmount}} \times 100$ |

**Example:**
- Opening balance: €10,000
- Income: €2,000
- Expenses: €500
- Contributed: €10,000 + €2,000 - €500 = €11,500
- Latest checkpoint value: €12,100
- Return: €12,100 - €11,500 = €600
- Return %: (€600 / €11,500) × 100 = 5.22%

---

## FAQ

### How is data backed up?

If using Supabase, daily automated backups are enabled by default. Check Supabase dashboard for backup settings and manual download options.

### Can I use this with a different LLM provider?

Yes! The `LlmProviderRouter` in `AiConfig.java` can be extended to support any OpenAI-compatible API. Add a new bean and update the router's logic.

### What if LM Studio is offline?

The chat endpoint will fail with a clear error message. Switch the chat provider to Groq from the `/ai/config` endpoint, and requests will route to the cloud API instead.

### Can multiple people use the same vault?

No, Vault is designed for single-user. All users share the same password and access all data equally. For multi-user support, a new architecture would be needed (per-user accounts, role-based access control).

### How do I reset my vault password?

Currently, there's no reset mechanism. You'd need to:
1. Manually delete the `vault_password_hash` row from `app_config` table (SQL)
2. Restart the application
3. The vault returns to `configured: false`
4. Call `/auth/setup` again with a new password

### What's the rate limit?

5 login/setup attempts per 15 minutes per IP address. Other endpoints have no rate limit.

### Is my data encrypted at rest?

No, data is stored as plain text in PostgreSQL. For sensitive deployments, use PostgreSQL's built-in encryption features or database-level encryption (Supabase provides this).

---

## License

[LICENSE](LICENSE) file in the repository.
