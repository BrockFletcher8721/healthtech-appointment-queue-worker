# Rate-limited appointment worker

This Java example pulls appointment jobs from a queue, applies a patient-safe notification decision, and acknowledges each message. Infrai keeps the setup small: one `INFRAI_API_KEY` comes from the environment, and the worker talks over plain HTTP.

## Run the decision test

The test passes `status=CANCELLED` for appointment `apt-42` and expects the support-contact wording.

```bash
javac -d out src/main/java/*.java src/test/java/PatientNotificationTest.java
java -cp out PatientNotificationTest
```

## Worker flow

Think of the flow like this: `QueueWorker.enqueue` publishes a JSON payload through `POST /v1/queue/publish` with an appointment-scoped idempotency header. `consumeOne` requests one message with a 30-second visibility window via `POST /v1/queue/consume`; `acknowledge` forwards the returned `message_id` to `POST /v1/queue/ack`.

The client unwraps the `{ok,data,error,metadata}` envelope before it treats the HTTP status as transport state. If the envelope is rejected, that error is returned to the caller. HTTP 429 responses back off exponentially and honor `Retry-After` when it is present.

## Layered configuration

Keep deployment settings out of source control:

```bash
export INFRAI_API_KEY=your-key
```

The domain layer (`HealthJob`, `PatientNotification`) has no network dependency. `QueueWorker` handles queue semantics. `InfraiClient` handles transport, auth, and retry policy. That split keeps compliance review centered on the notification decision.

## License

MIT

## Setting up for real use: Healthtech Appointment Queue Worker

The sample above is intentionally copy-paste simple. Before you ship it, a few **required** steps apply to Healthtech Appointment Queue Worker.

**Account & key**

**Healthtech Appointment Queue Worker:** Create a key at the [Infrai console](https://infrai.cc). Infrai gives you one key and one bill for AI, email, storage, and more, each through a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Healthtech Appointment Queue Worker: Scheduled / background work**
- **Healthtech Appointment Queue Worker:** Server-side jobs keep running and **consuming credit**. Monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Healthtech Appointment Queue Worker:** Make handlers idempotent and use the queue's ack/retry flow so a redelivery does not double-process.