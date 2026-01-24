# Mockavior — Contract-driven Mock Platform

Mockavior is a **contract-driven HTTP API mocking platform** designed for:

* local and dev environments
* integration testing
* emulation of unstable / external services
* Kafka emulation and polling
* controlled proxy (passthrough)

The platform is driven by **a single YAML contract (`mockapi.yml`)**, supports **hot reload**, **conditional routing**, **fallback behavior**, **Kafka emulation**, and **message polling**.

## 🧠 Core Idea

**The contract is the single source of truth**

Mockavior:

* does not store request state
* contains no business logic
* knows nothing about concrete APIs

It **executes the contract**.

## 🧩 REST Contract Emulation Architecture (short)

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
* **Reload = operation, not a side-effect**

## 🚀 Quick Start

### 1️⃣ Starting the Service

#### Option A — Locally (without Docker)

`java -jar mockavior.jar \`
`--mockavior.contract.path=/path/to/mockapi.yml`

#### Option B — Docker

`docker pull unisoft123/mockavior:latest`

`docker run -p 8080:8080 -v /path/to/mockapi.yml:/app/config/mockapi.yml unisoft123/mockavior`

Mockavior requires a contract file at startup.  
The container will not start without a mounted contract.

## 📄 Contract mockapi.yml

### Minimal Example

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
* priorities (`priority`)
* conditions (`when`)
* fixed and random delays (fixed + random)
* loading response body from file or inline (bodyFile + body)

### Example with Parameters

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

### Query Parameters

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

`*` → header must be present  
Headers are case-insensitive

### Priority Resolution

If multiple routes match:

* higher priority wins
* `when` condition must pass
* first matching route wins

## 🎭 Response Types

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

Template replacement is supported for:

* path params
* query params
* headers (in body / headers)

#### Loading body from file (bodyFile)

Instead of inline body, a file can be specified.

```yaml
response:
  type: mock
  status: 200
  bodyFile: responses/user.json
```

**Rules:**

- path is relative to contract directory
- file is read as bytes
- JSON, text, and binary are supported
- bodyFile has priority over body

If both are specified:

```yaml
body: { ignored: true }
bodyFile: responses/user.json
```

→ **bodyFile** will be used.

#### Response Delays (delay)

Mockavior supports fixed and random delays for HTTP responses.

**Fixed delay**

```yaml
response:
  type: mock
  status: 200
  delay: 500ms
  body:
    ok: true
```

The response will be sent after **500 ms**.

**Random delay**

```yaml
delay:
  random:
    min: 200ms
    max: 800ms
```

Actual delay is randomly selected in the range **[200ms, 800ms]**.

**Combined delay (fixed + random)**

```yaml
delay:
  fixed: 300ms
  random:
    min: 100ms
    max: 400ms
```

Actual delay: `fixed + random → [400ms … 700ms]`

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

## 🔁 Fallback Behavior

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

## 🔄 Contract Reload

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

### Get Current Contract

`GET /__mockavior__/contract`

Headers:

* Mockavior-Contract-Version: 12
* Content-Type: application/yaml

Body:
```yaml
version: 1
...
```

### Update Contract (PUT)

`PUT /__mockavior__/contract`  
`If-Match: 12`  
`Content-Type: text/plain`

Body:
```
<mockapi.yml contents>
```

Responses:

| Status | Meaning |
|------|--------|
| 200 | OK |
| 409 | Version conflict |
| 400 | Validation error |

### Optimistic Locking

* Full replace
* prevents overwriting concurrent changes
* version taken from Mockavior-Contract-Version

### Contract Validation

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

# Mockavior — Kafka Emulation & Polling

## 📌 Purpose

Kafka emulation in Mockavior is **not a Kafka replacement**, but is intended for:

- deterministic integration tests
- reproducible scenarios
- validating consumer reactions
- contract testing of event-driven systems

It is an **event store + scheduler**, driven by the contract.

---

## 🧠 Key Idea

> Kafka is a side-effect of the contract

HTTP → Kafka in Mockavior are **not directly coupled**.  
Kafka scenarios are defined in the contract and executed asynchronously.

---

## 🧩 Kafka Emulation Architecture

```
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

### Core Components

- `KafkaScenario` — scenario description
- `ScenarioExecutionRunner` — async executor
- `RuntimeScheduler` — manages delays
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

### Kafka Message Delays

Each Kafka message can have a delay before publishing.

**Fixed delay**

```yaml
delay: 1000ms
```

**Random delay**

```yaml
delay:
  random:
    min: 300ms
    max: 800ms
```

**Combined delay**

```yaml
delay:
  fixed: 500ms
  random:
    min: 200ms
    max: 600ms
```

Actual delay: `500ms + [200ms...600ms]`.

### Semantics

| Field | Meaning |
|------|--------|
| topic | Kafka topic |
| key | message key |
| value | payload (JSON) |
| delay | delay before publish |
| repeat | number of times to publish |

---

### Loading value from file (valueFile)

Kafka messages can load payload from file, similar to HTTP responses.

```yaml
valueFile: values/user-created.json
```

Semantics:

- file is read as bytes
- path is relative to contract
- valueFile has priority over value
- Kafka always stores bytes, without interpretation

If both are specified:

```yaml
value:
  id: 1
valueFile: values/user.json
```

→ **valueFile** is used.

## ▶️ Start Scenario

`POST /__mockavior__/kafka/start/{scenarioId}`

Response:

```json
{
  "executionId": "uuid",
  "scenarioId": "user-events",
  "state": "RUNNING"
}
```

---

## 📬 Polling API

### Decode modes (Admin API)

Kafka in Mockavior always stores bytes.  
Decoding is performed at the Admin API level.

| decode | Description |
|------|------------|
| none | raw bytes (Base64 only) |
| text | UTF-8 string |
| json | JSON (if possible) |

Example:

`GET /__mockavior__/kafka/poll/user.created?decode=json`

Response:

```json
{
  "value": {
    "raw": "eyJpZCI6MSwibmFtZSI6IkFsaWNlIn0=",
    "decoded": { "id": 1, "name": "Alice" },
    "decode": "json",
    "source": "inline"
  }
}
```

Important:

- Kafka store never holds decoded data
- decoded is a presentation concern
- raw is always available

---

## 🧱 Guarantees

✔ deterministic order per topic  
✔ no shared state outside store  
✔ no real Kafka dependency  
✔ reproducible tests  
✔ snapshot-safe reload

---

## ❌ Limitations

- no partitions
- no consumer groups
- no offset management
- no retention
- no exactly-once

> This is a **test double**, not a broker.

---

## 🧠 Philosophy

> Kafka is not infrastructure, it is an event contract.

Kafka = bytes. Always.  
Data interpretation is the client's responsibility.

Mockavior intentionally separates:

- storage (raw bytes)
- presentation (decoded via Admin API)

This avoids:

- false expectations of formats
- magic transformations
- implicit serialization

And makes Kafka contracts honest and portable.
