# Mockavior — Contract-driven Mock Platform

Mockavior is a **contract-driven HTTP API mocking platform** designed for:

* local and dev environments
* integration testing
* emulation of unstable / external services
* Kafka emulation and polling
* controlled proxy (passthrough)

The platform is managed by **a single YAML contract (`mockapi.yml`)**, supports **hot reload**, **conditional routing**, and **fallback behavior**.

## 🧠 Core Idea

**The contract is the single source of truth**

Mockavior:

* does not store request state
* contains no business logic
* knows nothing about specific APIs

It **executes the contract**.

## 🧩 Architecture for REST Contract Emulation (short)

HTTP request

↓

HttpTransportAdapter

↓

GenericRequest

↓

BehaviorEngine

↓

Router → Match → Behavior

↓

BehaviorResult

↓

HTTP response

Key principles:

* **Immutable ContractSnapshot**
* **Atomic snapshot swap**
* **In-flight requests are not broken**
* **Reload = an operation, not a side-effect**

## 🚀 Quick Start

### 1️⃣ Service startup

#### Option A — locally (without Docker)

`java -jar mockavior.jar \`  
`--mockavior.contract.path=/path/to/mockapi.yml`

#### Option B — Docker

`docker pull unisoft123/mockavior:latest`

`docker run -p 8080:8080 -v /path/to/mockapi.yml:/app/config/mockapi.yml unisoft123/mockavior`

Mockavior requires a contract file at startup.  
The container will not start without a mounted contract.

## 📄 Contract mockapi.yml

### Minimal example

```yaml
version: 1  
settings:  
  mode: STRICT  
  defaultStatus: 404

endpoints: 
  - id: health 
    request:  
      method: GET  
      path: /health  
    response:  
      type: mock  
      status: 200  
      body: "OK"
```

## 🔀 Routing

### Supported
* `literal paths`  
  `/health`
* `parameterized paths`  
  `/users/{id}`
* `priorities (priority)`
* `conditions (when)`

### Example with parameters

```yaml
- id: get-user  
  priority: 10  
  request:  
    method: GET  
    path: /users/{id}  
  response:  
    type: mock  
    status: 200  
    body:  
      id: "{id}"
```

Request:  
`GET /users/123`

Response:
```json
{
  "id": "123"
}
```

## 🧠 Conditions (when)

### Query parameters

```yaml
when: 
  query:  
    active: true
```

`GET /users/123?active=true`

### Headers

```yaml
when:  
  headers:  
    X-Role: admin  
    X-Debug: "*"
```

\* → parameter must be present  
headers are case-insensitive

### Conflict priority

If multiple routes match:

* higher priority
* when must match
* first matched wins

## 🎭 Response types

### 1️⃣ mock

```yaml
response:  
  type: mock  
  status: 200  
  headers:  
    X-User-Id: "{id}"  
  body:  
    id: "{id}"
```

Template replacement is supported:

* path params
* query params
* headers (in body / headers)

### 2️⃣ error

```yaml
response:  
  type: error  
  status: 500
```

Used for:

* forced failures
* chaos testing
* negative scenarios

### 3️⃣ proxy

```yaml
response: 
  type: proxy
```

## 🔁 Fallback behavior

Fallback is applied only if no endpoint matches.

### STRICT (default)

```yaml
settings:  
  mode: STRICT  
  defaultStatus: 404
```

### PASSTHROUGH

```yaml
settings:  
  mode: PASSTHROUGH  
  proxy:  
    baseUrl: http://httpbin.org
```

`GET /status/418 → forwarded to httpbin.org/status/418`

## 🔄 Contract reload

### Automatically

* on mockapi.yml change
* via WatchService
* without service restart

### Manually

`POST /__mockavior__/reload`

Response:

```json
{  
  "status": "SUCCESS",  
  "source": "/path/to/mockapi.yml",  
  "snapshotVersion": 12  
}
```

## 🛠 Admin API

### Get current contract

`GET /__mockavior__/contract`

Headers:

* Mockavior-Contract-Version: 12
* Content-Type: application/yaml

Body:  
`version: 1`  
`…`

### Update contract (PUT)

`PUT /__mockavior__/contract`  
`If-Match: 12`  
`Content-Type: text/plain`

Body:  
`<mockapi.yml file content>`

Possible responses:

| Status | Meaning |
|------|--------|
| 200 | OK |
| 409 | Version conflict |
| 400 | Validation error |

### Optimistic Locking

* full replace
* protection from overwriting changes
* version is taken from Mockavior-Contract-Version

### Contract validation

`POST /__mockavior__/contract/validate`  
`Content-Type: text/plain`

Response:

```json
{  
  "status": "VALID",  
  "message": "Contract validation successful"  
}
```

or

```json
{  
  "code": "VALIDATION_ERROR",  
  "message": "Unknown response.type: foo"  
}
```

❌ ErrorResponse (unified format)

```json
{  
  "code": "VERSION_CONFLICT",  
  "message": "Contract version mismatch",  
  "currentVersion": "13"  
}
```


# Kafka Emulation & Polling

## 📌 Purpose

Kafka emulation in Mockavior is **not intended to replace Kafka**, but for:

- deterministic integration tests
- reproducible scenarios
- consumer behavior verification
- contract testing of event-driven systems

It is an **event store + scheduler**, managed by the contract.

---

## 🧠 Key idea

> Kafka is a side-effect of the contract

HTTP → Kafka in Mockavior are **not directly coupled**.  
Kafka scenarios are described in the contract and executed asynchronously.

---

## 🧩 Kafka emulation architecture

```text
ContractSnapshot
   └── kafka.scenarios
         └── KafkaScenario
               └── KafkaMessage (topic, key, value, delay, repeat)
                        ↓
               ScenarioExecutionRunner (async)
                        ↓
               InMemoryKafkaStore
                        ↓
               KafkaPollController
```

### Main components

- `KafkaScenario` — scenario description
- `ScenarioExecutionRunner` — async executor
- `RuntimeScheduler` — delay management
- `InMemoryKafkaStore` — thread-safe store
- `KafkaPollController` — HTTP polling API

---

## 📄 Contract: kafka section

### Example

```yaml
kafka:
  scenarios:
    user-events:
      repeat: 1
      messages:
        - topic: user.created
          key: user-1
          value:
            id: 1
            name: John
          delay: 0

        - topic: user.updated
          key: user-1
          value:
            name: John Updated
          delay: 1000
```

### Semantics

| Field | Meaning |
|------|--------|
| topic | Kafka topic |
| key | message key |
| value | payload (JSON) |
| delay | delay before publish (ms) |
| repeat | number of sends |

---

## ▶️ Start scenario

`POST /__mockavior__/kafka/start/{scenarioId}`

Response:

```json
{
  "executionId": "uuid",
  "scenarioId": "user-events",
  "state": "RUNNING"
}
```

📌 Scenario:

- executes **asynchronously**
- does not block HTTP
- can publish delayed messages

---

## ⏹ Stop scenario

`POST /__mockavior__/kafka/stop/{executionId}`

Used for:

- cleanup
- emergency stop
- test scenarios

---

## 📬 Polling API (core)

### Peek (non-destructive)

`GET /__mockavior__/kafka/poll/{topic}`

Response:

```json
{
  "topic": "user.created",
  "count": 1,
  "messages": [
    {
      "topic": "user.created",
      "key": "user-1",
      "value": { "id": 1 },
      "repeat": 1,
      "delay": "PT0S"
    }
  ]
}
```

📌 Messages are **not removed**.

---

### Take (destructive)

`POST /__mockavior__/kafka/poll/{topic}/take`

Response:

```json
{
  "topic": "user.created",
  "key": "user-1",
  "value": { "id": 1 },
  "repeat": 1,
  "delay": "PT0S"
}
```

- message is **removed**
- FIFO
- if no messages → `204 No Content`

---

### Clear topic

`POST /__mockavior__/kafka/poll/{topic}/clear`

Response:

```json
{ "cleared": true }
```

---

## 🔁 Asynchronicity

- all Kafka messages are published via `RuntimeScheduler`
- delay from contract is used
- scenarios can run in parallel
- store is thread-safe (`ConcurrentHashMap + Queue`)

---

## 🧪 Integration tests

Typical flow:

1. load contract
2. `POST /kafka/start/{scenario}`
3. `sleep()` (or polling loop)
4. `POST /poll/{topic}/take`
5. assert payload
6. `GET /poll/{topic}` → count == 0

Example:

```java
Map<String, Object> msg =
  client.post()
        .uri("/__mockavior__/kafka/poll/user.created/take")
        .retrieve()
        .bodyToMono(Map.class)
        .block();

assertThat(msg.get("key")).isEqualTo("user-1");
```

---

## 🧱 Guarantees

✔ deterministic order per topic  
✔ no shared state outside store  
✔ no real Kafka dependency  
✔ reproducible tests  
✔ snapshot-safe (reload does not break runner)

---

## ❌ Intentional limitations

- no partitions
- no consumer groups
- no offset management
- no retention
- no exactly-once

> This is a **test double**, not a broker.

---

## 🧭 When to use

✔ contract testing  
✔ async workflows  
✔ saga testing  
✔ consumer simulation  
✔ CI pipelines

❌ performance testing  
❌ real Kafka behavior validation

---

## 🧠 Philosophy

> Kafka is not infrastructure, it is an event contract.

Mockavior makes events **part of the API contract**.

---

### 🧱 Current MVP boundaries

✅ Already implemented:

* HTTP mock
* proxy
* error responses
* conditional routing
* hot reload
* optimistic locking
* immutable snapshots
* admin API

❌ Not yet (intentionally):

* partial contract merge
* UI
* auth / RBAC
* rate limiting
* metrics / tracing
* OpenAPI export
