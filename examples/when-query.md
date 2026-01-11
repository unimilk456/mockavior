# Routing with Query Parameters

This example demonstrates routing based on query parameters.

## Contract Snippet

```yaml
routes:
  - id: active-users
    priority: 100
    request:
      method: GET
      path: /users
    when:
      query:
        active: true
    response:
      status: 200
      body:
        filter: active
```

### Behavior
- Matches `GET /users?active=true`
- Any other value does not match