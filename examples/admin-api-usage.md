# Admin API Usage Example

This example shows how to interact with the Admin API.

## Get Current Contract

```bash
curl -i http://localhost:8080/__mockavior__/contract
```
Response headers include:
```
Mockavior-Contract-Version: v12
```

## Validate Contract

```bash
curl -X POST \
  -H "Content-Type: text/plain" \
  --data-binary @mockapi.yml \
  http://localhost:8080/__mockavior__/contract/validate
```

No reload is performed.