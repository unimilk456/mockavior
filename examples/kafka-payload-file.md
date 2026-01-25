# Kafka Message Payload from File

This example shows how to store **Kafka message payloads** in external files.

## Contract Snippet

```yaml
kafka:
  topics:
    - name: user-events
      when:
        headers:
          type: USER_CREATED
      produce:
        valueFile: kafka/user-created.json
```

### Payload File (kafka/user-created.json)

```json
{
  "event": "USER_CREATED",
  "source": "mockavior",
  "version": 1
}
```

### Behavior
- Kafka message value is loaded from a file
- Useful for large or reusable payloads
- Keeps contracts clean
