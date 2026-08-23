# support-assistant

A Spring AI–based internal helpdesk chat assistant. It answers support questions using
RAG over a small knowledge base (pgvector), can call tools to look up/create/update
support tickets, remembers conversation history (Postgres-backed), and can be backed by
either a local Ollama model or Anthropic's Claude.

This doc exists so future-me can re-open this repo cold and immediately remember how it
fits together, what to run, and what the prompts/tools/endpoints are.

---

## 1. What this project actually does (flow)

```
Client
  │  POST /chat?conversationId=<id>&provider=<ollama|claude>
  │  body: raw text message
  ▼
ChatController.chat()
  │
  ├─ picks a ChatClient bean: "ollamaChatClient" or "claudeChatClient"
  │   (defaults to ollama if `provider` is omitted)
  │
  ├─ validates message (non-blank, <= 2000 chars)
  │
  ▼
ChatClient.prompt().user(message)
  │
  ├─ MessageChatMemoryAdvisor   → loads/saves last 20 messages for this
  │                                conversationId (chat memory is JDBC-backed,
  │                                stored in Postgres, schema auto-created)
  │
  ├─ QuestionAnswerAdvisor      → embeds the query (Ollama nomic-embed-text),
  │                                does a similarity search against pgvector's
  │                                `vector_store` table, and stuffs the top
  │                                matching KB chunks into the prompt context
  │
  ├─ defaultTools(HelpdeskTools)→ model may call: getTicketStatus, createTicket,
  │                                updateTicketStatus, listTickets
  │
  ▼
LLM (Ollama llama3.2, or Anthropic claude-sonnet-4-6)
  │
  ▼
Response streamed back as text/event-stream (Flux<String>)
```

On every app startup, `KnowledgeBaseLoader` (a `CommandLineRunner`) checks whether the
vector store already has data (via a similarity search for "password"); if empty, it
reads every file under `src/main/resources/kb/*.txt`, chunks/embeds them, and writes
them into the `vector_store` table in Postgres. So the KB only gets (re-)ingested once,
the first time you point the app at an empty database.

---

## 2. Tech stack

- **Java 17**, Spring Boot 4.1.0, Gradle (wrapper included, no local Gradle needed)
- **Spring AI 2.0.0**
  - `spring-ai-starter-model-ollama` — chat + embeddings via local Ollama
  - `spring-ai-starter-model-anthropic` — chat via Claude
  - `spring-ai-starter-vector-store-pgvector` + `spring-ai-vector-store-advisor` — RAG
  - `spring-ai-starter-model-chat-memory-repository-jdbc` — conversation memory persisted in Postgres
- **Postgres + pgvector** (`pgvector/pgvector:pg16` via Docker)
- **Ollama** (native Windows install, not dockerized) running:
  - `llama3.2` — chat model
  - `nomic-embed-text` — embedding model (768 dims)
- `spring-boot-docker-compose` is on the classpath (`developmentOnly`), so **Spring Boot
  auto-detects `docker-compose.yml` in the project root and starts/stops the pgvector
  container itself** when you run the app via `bootRun`/IDE. You generally don't need to
  run `docker compose up` manually.

---

## 3. Prerequisites (one-time setup)

1. **Docker Desktop** running.
2. **Ollama** installed and running natively (`ollama serve`, or the tray app), with
   models pulled:
   ```
   ollama pull llama3.2
   ollama pull nomic-embed-text
   ```
3. **Anthropic API key** — required if you want to use `provider=claude`. Set it as an
   environment variable before starting the app:
   ```powershell
   $env:ANTHROPIC_API_KEY = "sk-ant-..."
   ```
   ⚠️ `application.properties` has `spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}`
   with **no default**. If this env var isn't set, the Anthropic autoconfiguration bean
   fails to resolve and **the app won't start at all** — not even for the Ollama path.
   Always set it (a dummy value like `sk-ant-placeholder` is enough if you only plan to
   use the Ollama provider).

---

## 4. Running the project

### Option A — the script (`run.ps1`)
```powershell
./run.ps1
```
This checks Docker, starts the pgvector container, waits for it to be healthy, makes
sure Ollama is up and the required models are pulled, then runs `./gradlew.bat bootRun`.

### Option B — manual
```powershell
docker compose up -d          # optional — Spring Boot's docker-compose support will also do this
./gradlew.bat bootRun
```

The app starts on **http://localhost:9090**.

---

## 5. Configuration reference (`application.properties`)

| Property | Value | Notes |
|---|---|---|
| `server.port` | `9090` | |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | native Ollama, not containerized |
| `spring.ai.ollama.chat.options.model` | `llama3.2` | |
| `spring.ai.ollama.chat.options.temperature` | `2.0` | unusually high — very random/creative output, intentional for experimentation |
| `spring.ai.ollama.embedding.options.model` | `nomic-embed-text` | 768-dim embeddings |
| `spring.ai.anthropic.api-key` | `${ANTHROPIC_API_KEY}` | see prerequisites above |
| `spring.ai.anthropic.chat.options.model` | `claude-sonnet-4-6` | |
| `spring.ai.anthropic.chat.options.temperature` | `0.7` | |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5434/support-assistant` | maps to docker-compose port 5434→5432 |
| `spring.ai.chat.memory.repository.jdbc.initialize-schema` | `always` | recreates chat memory tables on each boot |
| `spring.ai.vectorstore.pgvector.*` | HNSW / cosine / 768 dims | must match the embedding model's dimensionality |

---

## 6. System prompts

Both `ollamaChatClient` and `claudeChatClient` (in `AiConfig.java`) use the same system
prompt:

```
You are a helpful support assistant for an internal helpdesk app.
Only answer questions related to account issues, password resets,
refunds, shipping, and support tickets.
If asked something outside this scope, politely say you can only help
with helpdesk-related questions.
Answer using the provided context when it's relevant.
If the answer isn't in the context, say you don't have that information
rather than guessing.
Use the available tools when the user asks about ticket status or wants to create a ticket.
```

Both clients share the same advisors (chat memory + RAG) and the same tool set
(`HelpdeskTools`) — the only difference is the underlying model.

---

## 7. Tools (`HelpdeskTools.java`)

Backed by an in-memory `TicketService` (fake data, resets on every restart — seeded
with tickets `482`, `501`, `512`).

| Tool | Description |
|---|---|
| `getTicketStatus(ticketId)` | Look up a single ticket by ID |
| `createTicket(description)` | Create a new ticket (status starts at `Open`) |
| `updateTicketStatus(ticketId, newStatus)` | Valid statuses: `Open`, `In Progress`, `Resolved`, `Closed` |
| `listTickets(statusFilter)` | List all tickets, optionally filtered by status |

The LLM decides when to call these based on the system prompt instruction ("use tools
when the user asks about ticket status or wants to create a ticket").

---

## 8. Knowledge base (RAG source docs)

Located at `src/main/resources/kb/*.txt`, loaded into pgvector on first run:

- **password-reset.txt** — how to reset password via "Forgot Password", 30-min link expiry
- **refund-policy.txt** — 30-day refund window, email billing@example.com, 5-7 day processing
- **shipping-times.txt** — standard 5-7 days, express 1-2 days, international 10-15 days

To force a re-ingest (e.g. after editing a KB file), drop the `vector_store` table or
wipe the `support-assistant-pgdata` Docker volume — the loader only skips ingestion if a
similarity search for "password" already returns results.

---

## 9. Endpoints

### `POST /chat`
Send a chat message. Response is streamed (`text/event-stream` via `Flux<String>`).

**Query params:**
- `conversationId` (required) — groups messages into a conversation for memory purposes
- `provider` (optional, default `ollama`) — `ollama` or `claude`

**Body:** raw text (not JSON) — the user's message.

```powershell
curl -X POST "http://localhost:9090/chat?conversationId=test1&provider=ollama" `
  -H "Content-Type: text/plain" `
  --data-raw "How do I reset my password?"

curl -X POST "http://localhost:9090/chat?conversationId=test1&provider=claude" `
  -H "Content-Type: text/plain" `
  --data-raw "What's the status of ticket 482?"
```

### `GET /chat/debug/search`
Directly queries the vector store (bypasses the LLM entirely) — useful for checking
what context RAG would actually retrieve for a given query.

```powershell
curl "http://localhost:9090/chat/debug/search?query=refund"
```

---

## 10. Gotchas / things that will bite future-me

- **Port drift**: server port has moved from 8090 → 9090, and the Postgres port from
  5433 → 5434 across commits. If something "isn't working," check
  `application.properties` first for the current ports rather than assuming.
- **ANTHROPIC_API_KEY must be set even to run the Ollama-only path** (see §3).
- **Chat memory schema resets on every boot** (`initialize-schema=always`) — don't
  expect conversation history to survive a restart.
- **KB re-ingestion is skip-based, not hash-based** — editing a KB `.txt` file and
  restarting will *not* pick up the change unless the vector store is empty first.
- **`spring-boot-docker-compose`** means `docker-compose.yml` is auto-started/stopped by
  Spring Boot itself during `bootRun` — don't be surprised if the container appears/
  disappears without you calling `docker compose` directly.
