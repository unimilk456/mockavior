# Basic Routing Example

This example demonstrates the simplest routing configuration.

## Contract Snippet

```yaml
routes:
  - id: get-users
    priority: 100
    request:
      method: GET
      path: /users
    response:
      status: 200
      body:
        users: []
```

### Behavior
- Matches ```GET /users```
- No conditions
- Always returns an empty users list
