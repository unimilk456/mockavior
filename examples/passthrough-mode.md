# Passthrough Mode Example

Passthrough mode proxies unmatched requests to a real backend.

## Settings

```yaml
settings:
  fallbackMode: PASSTHROUGH
  backend:
    baseUrl: http://localhost:8081
```

### Behavior
- Routes are evaluated first
- If no route matches:
  - Request is proxied to the backend
  - Original method, headers, and body are preserved