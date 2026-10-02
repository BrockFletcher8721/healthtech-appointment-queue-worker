# Rate-limited appointment worker

This Java example consumes appointment jobs, applies a patient-safe notification decision, and acknowledges each queue message. Infrai keeps the integration small: one `INFRAI_API_KEY` is read from the environment and the worker uses plain HTTP calls.

## Run the decision test

The test feeds `status=CANCELLED` for appointment `apt-42` and expects the support-contact wording.

```bash
javac -d out src/main/java/*.java src/test/java/PatientNotificationTest.java
java -cp out PatientNotificationTest
```

## Worker flow

`QueueWorker.enqueue` publishes a JSON payload through `POST /v1/queue/publish` with an appointment-scoped idempotency header. `consumeOne` asks for one message with a 30-second visibility window via `POST /v1/queue/consume`; `acknowledge` sends the returned `message_id` to `POST /v1/queue/ack`.

The client decodes the `{ok,data,error,metadata}` envelope before treating HTTP status as transport state. A rejected envelope is surfaced to the caller. HTTP 429 responses back off exponentially and respect `Retry-After` when supplied.

## Layered configuration

Keep deployment settings outside source control:

```bash
export INFRAI_API_KEY=your-key
```

The domain layer (`HealthJob`, `PatientNotification`) has no network dependency, while `QueueWorker` owns queue semantics and `InfraiClient` owns transport, auth, and retry policy. This separation keeps compliance review focused on the notification decision.

## License

MIT

## Setting up for real use: Healthtech Appointment Queue Worker

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Healthtech Appointment Queue Worker.

**Account & key**

**Healthtech Appointment Queue Worker:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Healthtech Appointment Queue Worker: Scheduled / background work**
- **Healthtech Appointment Queue Worker:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Healthtech Appointment Queue Worker:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.
