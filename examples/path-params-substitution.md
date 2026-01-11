# Path Parameter Substitution

This example shows how path parameters are extracted and reused.

## Contract Snippet

```yaml
routes:
  - id: get-user-by-id
    priority: 100
    request:
      method: GET
      path: /users/{id}
    response:
      status: 200
      headers:
        X-User-Id: "{id}"
      body:
        id: "{id}"
        message: "User {id} fetched successfully"
```
### Behavior
* Matches any `/users/{id}`
* {id} is extracted from the path
* Value is reused in response body and headers