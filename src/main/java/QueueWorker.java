public final class QueueWorker {
    private static final String QUEUE = "appointment-jobs";
    private final InfraiClient client;
    public QueueWorker(InfraiClient client) { this.client = client; }

    public String enqueue(HealthJob job) throws Exception {
        String payload = "{\"appointmentId\":\"" + job.appointmentId() + "\",\"patientId\":\"" + job.patientId() + "\",\"status\":\"" + job.status() + "\"}";
        return client.call("/v1/queue/publish", "POST", "{\"queue\":" + quote(QUEUE) + ",\"payload\":" + quote(payload) + "}", "appointment-" + job.appointmentId());
    }

    public String consumeOne() throws Exception {
        return client.call("/v1/queue/consume", "POST", "{\"queue\":" + quote(QUEUE) + ",\"max_messages\":1,\"visibility_timeout\":30}", "consume-worker");
    }

    public String acknowledge(String messageId) throws Exception {
        return client.call("/v1/queue/ack", "POST", "{\"queue\":" + quote(QUEUE) + ",\"message_id\":" + quote(messageId) + "}", "ack-" + messageId);
    }

    private static String quote(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
}
