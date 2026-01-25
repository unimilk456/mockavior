# Random Response Delay

This example shows how to configure **randomized delays** for responses.

## Contract Snippet

```yaml
routes:
  - id: delayed-random
    priority: 100
    request:
      method: GET
      path: /slow
    response:
      status: 200
      delay:
        random:
          min: 100ms
          max: 800ms
      body:
        result: ok
```

### Behavior
- Each request is delayed by a random value
- Delay range is between **100ms and 800ms**
- Useful for latency simulation and load testing
