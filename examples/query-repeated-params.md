# Query Matching with Repeated Parameters

This example demonstrates matching **repeated query parameters** using `any` and `all` rules.

## Contract Snippet

```yaml
routes:
  - id: tag-any
    priority: 200
    request:
      method: GET
      path: /test
    when:
      query:
        tag:
          any: ["as", "ab"]
    response:
      status: 200
      body:
        matched: tag-any

  - id: tag-all
    priority: 300
    request:
      method: GET
      path: /test
    when:
      query:
        tag:
          all: ["as", "ab"]
    response:
      status: 200
      body:
        matched: tag-all
```

### Behavior
- `any` matches if at least one value is present
- `all` matches only if **all required values** are present
- Priority determines which rule wins
