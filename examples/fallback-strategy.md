# Fallback Strategy Example

This example demonstrates fallback behavior when no routes match.

## Settings

```yaml
settings:
  fallbackMode: STRICT
```

### Behavior
- If no route matches the request
- Mockavior returns ```404 Not Found```
- No request is forwarded to backend
