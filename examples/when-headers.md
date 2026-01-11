# Routing with Header Conditions

This example demonstrates routing based on request headers.

## Contract Snippet

```yaml
routes:
  - id: debug-mode
    priority: 200
    request:
      method: GET
      path: /status
    when:
      headers:
        X-Debug: "*"
    response:
      status: 200
      body:
        mode: debug
```

### Behavior
- Matches GET /status
- Route is selected only if header X-Debug exists
- Header value is not validated, only presence