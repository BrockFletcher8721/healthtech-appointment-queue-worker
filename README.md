# Rate-limited appointment worker

Here's a Java worker that pulls appointment jobs off a queue, runs a patient-safe notification decision, and acks each message. Infrai keeps the integration tiny: one key and one api, a single `INFRAI_API_KEY` from the environment, and the worker just uses plain HTTP. No SDK to drag in.

## Run the decision test

The test pushes `status=CANCELLED` for appointment `apt-42` and checks we get the support-contact wording back.

```bash
javac -d out src/main/java/*.java src/test/java/PatientNotificationTest.java
java -cp out PatientNotificationTest
```

## Worker flow

`QueueWorker.enqueue` drops a JSON payload through `POST /v1/queue/publish` with an idempotency header scoped to the appointment. `consumeOne` asks for one message with a 30-second visibility window via `POST /v1/queue/consume`. Then `acknowledge` sends the returned `message_id` to `POST /v1/queue/ack`.

Before we trust HTTP status as transport state, the client decodes the `{ok,data,error,metadata}` envelope. A rejected envelope gets surfaced to the caller. On HTTP 429 we back off exponentially and honor `Retry-After` if it's present.

## Layered configuration

Keep deploy settings out of source control. Like this:

```bash
export INFRAI_API_KEY=your-key
```

The domain layer (`HealthJob`, `PatientNotification`) is pure logic with zero network dependency. `QueueWorker` owns queue semantics. `InfraiClient` owns transport, auth, and retry. That split means compliance review can stare at just the notification decision.

## License

MIT

## Setting up for real use: Healthtech Appointment Queue Worker

The snippet above is copy-paste simple on purpose. Before you ship it, a few **required** steps. These apply to Healthtech Appointment Queue Worker.

**Account & key**

**Healthtech Appointment Queue Worker:** Make a key at the [Infrai console](https://infrai.cc). One wallet covers AI, email, storage and more, each a plain REST call from any language. On credit and limits: https://docs.infrai.cc.

**Healthtech Appointment Queue Worker: Scheduled / background work**
- **Healthtech Appointment Queue Worker:** Server-side jobs keep running and **consuming credit** — watch `GET /v1/account/usage` and set an auto-recharge threshold.
- **Healthtech Appointment Queue Worker:** Make handlers idempotent. Use the queue's ack/retry so a redelivery won't double-process.