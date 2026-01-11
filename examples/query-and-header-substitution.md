# Query and Header Substitution

This example demonstrates using values from query parameters
and request headers in the response.

## Contract Snippet

```yaml
routes:
  - id: echo-request-context
    priority: 100
    request:
      method: GET
      path: /echo
    when:
      headers:
        X-Request-Id: "*"
      query:
        client: "*"
    response:
      status: 200
      headers:
        X-Echo-Request-Id: "{headers.X-Request-Id}"
      body:
        client: "{query.client}"
        requestId: "{headers.X-Request-Id}"
        message: "Request from {query.client}"
```
### Behavior

* Requires header `X-Request-Id`
* Requires query parameter client
* Values are substituted into:
  * response headers
  * response body