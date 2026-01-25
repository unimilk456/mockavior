# External Response Body Files

This example shows how to store **HTTP response bodies** in external files.

## Contract Snippet

```yaml
routes:
  - id: external-body
    request:
      method: GET
      path: /users
    response:
      status: 200
      bodyFile: bodies/users.json
```

### Body File (bodies/users.json)

```json
{
  "users": [
    { "id": 1, "name": "Alice" },
    { "id": 2, "name": "Bob" }
  ]
}
```

### Behavior
- Response body is loaded from an external JSON file
- Improves contract readability
- Allows reuse across routes
