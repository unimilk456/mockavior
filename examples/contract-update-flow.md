# Contract Update Flow (Optimistic Locking)

This example illustrates safe contract updates using optimistic locking.

## Step 1: Get Current Version

```bash
curl -i http://localhost:8080/__mockavior__/contract
```

```Mockavior-Contract-Version: v12```

## Step 2: Update Contract
```bash
curl -X PUT \
  -H "If-Match: v12" \
  -H "Content-Type: text/plain" \
  --data-binary @mockapi.yml \
  http://localhost:8080/__mockavior__/contract
```

### Possible Outcomes
- `200 OK` — contract replaced successfully
- `409 Conflict` — version mismatch, update rejected