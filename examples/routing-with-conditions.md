# Routing with Conditions

This example shows routing based on headers and query parameters.

## Contract Snippet

```yaml
routes:
  - id: debug-users
    priority: 200
    request:
      method: GET
      path: /users
    when:
      headers:
        X-Debug: "*"
      query:
        active: true
    response:
      status: 200
      body:
        debug: true
```

### Behavior
- Matches only if header ```X-Debug``` exists
- Requires query parameter ```active=true```
- Higher priority wins if multiple routes match