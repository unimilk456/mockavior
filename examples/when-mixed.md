# Routing with Mixed Conditions

This example combines headers and query conditions.

## Contract Snippet

```yaml
routes:
  - id: admin-view
    priority: 300
    request:
      method: GET
      path: /users
    when:
      headers:
        X-Role: admin
      query:
        extended: "*"
    response:
      status: 200
      body:
        view: admin-extended
```
### Behavior
* Requires header `X-Role=admin`
* Requires query parameter extended to exist 
* Higher priority than other /users routes