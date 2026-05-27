<div align="center">

# NION Orchestration Engine (Spring Boot)

### Enterprise AI Program Manager · Modular Monolith · v2.0

Structured intelligence from unstructured project communications — now rebuilt with Spring Boot, PostgreSQL, MongoDB, Redis, and a Spring-ready LLM adapter.

</div>

---

## 1. Project Overview

**NION** (Networked Intelligence Orchestration Node) ingests unstructured project communications and produces structured intelligence such as action items, risks, decisions, and escalation plans. This rebuild modernizes the engine for backend placements with production-ready architecture, security, and scalability.

### What’s New in the Spring Boot Rebuild
- Modular monolith with clear orchestration, persistence, and API boundaries
- JWT security + RBAC (admin/analyst/viewer)
- Multi-tenant aware data model
- Async orchestration workflow with audit trail
- MongoDB for raw message payloads, PostgreSQL for structured outputs
- Redis caching for knowledge retrieval
- LLM integration with fallback mode
- OpenAPI documentation and Docker Compose

---

## 2. System Architecture (L1 → L2 → L3)

```
L1 Orchestrator (routing plan)
  ├─ L2 Tracking/Execution
  ├─ L2 Communication/Collaboration
  └─ L2 Learning/Improvement
        └─ L3 specialist agents
Cross-cutting: knowledge retrieval + evaluation
```

**Visibility enforcement** is encoded in the Java registry: L1 only sees L2 domains, L2 only sees its domain agents.

---

## 3. Tech Stack (and Why)

| Layer | Technology | Why It’s Used |
|---|---|---|
| API | Spring Boot Web | Fast REST APIs + production ecosystem |
| Security | Spring Security + JWT | Stateless auth, RBAC, secure APIs |
| Relational DB | PostgreSQL + JPA | Structured orchestration data |
| Document DB | MongoDB | Raw unstructured message payloads |
| Cache | Redis | Low-latency knowledge retrieval |
| AI | OpenAI REST (Spring AI-ready adapter) | Clean LLM abstraction + fallback |
| Docs | Springdoc OpenAPI | Auto-generated API docs |
| DevOps | Docker Compose | One-command local stack |
| Testing | JUnit + Testcontainers | Integration tests with real DBs |

---

## 4. Repository Structure

```
src/main/java/com/ainions/nion
  api/                → REST controllers
  orchestration/      → L1/L2/L3 engine
  domain/             → JPA entities + enums
  document/           → MongoDB documents
  repository/         → Spring Data repositories
  security/           → JWT + RBAC
  tenant/             → Tenant context + filter
  llm/                → LLM adapter + JSON schema validation
  notification/       → Webhook notifications
  util/               → Audit logs, mappers
src/main/resources
  application.yml     → config
  prompts/            → prompt templates + JSON schemas
```

---

## 5. Setup & Installation

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker + Docker Compose

### Step 1 — Start Infrastructure
```bash
docker compose up -d
```

### Step 2 — Configure Environment
```bash
export NION_JWT_SECRET="dev-secret-change-me-dev-secret-change-me"
export NION_LLM_PROVIDER=mock   # set to openai to enable LLM
export OPENAI_API_KEY=sk-...
```

### Step 3 — Run the App
```bash
mvn spring-boot:run
```

### Step 4 — Swagger Docs
```
http://localhost:8080/swagger-ui/index.html
```

---

## 6. Core APIs

| Endpoint | Description |
|---|---|
| `POST /api/v1/auth/register` | Create user + get token |
| `POST /api/v1/auth/login` | Get JWT |
| `POST /api/v1/messages` | Submit message, start orchestration |
| `GET /api/v1/runs/{id}` | Run status + summary |
| `GET /api/v1/runs/{id}/map` | Orchestration map |
| `GET /api/v1/runs/compare` | Compare two runs |
| `GET /api/v1/agents` | Agent registry |
| `GET /api/v1/knowledge` | Knowledge base |
| `GET /api/v1/prompts` | Prompt templates |

---

## 7. Testing

```bash
mvn test
```

---

## 8. Resume-Ready Additions

- Multi-tenant architecture for enterprise deployments
- Prompt template versioning in DB
- Webhook notification system for orchestration completion
- Run comparison endpoint for audit and QA
- JSON schema validation for every LLM response

---

## 9. Notes

The legacy Python implementation remains in the repository for reference, but the production-grade Spring Boot engine is now the primary implementation.

---

**Author:** Sonu Kumar (Backend Developer)
