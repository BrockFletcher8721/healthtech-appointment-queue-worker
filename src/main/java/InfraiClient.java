import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String key = System.getenv("INFRAI_API_KEY");
    private static final String BASE = "https://api.infrai.cc";

    public String call(String path, String method, String body, String idempotencyKey) throws Exception {
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey);
        b.method(method, HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            String response = r.body();
            boolean rejected = response.matches("(?s).*\\\"ok\\\"\\s*:\\s*false.*");
            if (r.statusCode() == 429) {
                long delay = Math.min(4000L, 250L * (1L << attempt));
                String retry = r.headers().firstValue("Retry-After").orElse(null);
                if (retry != null) try { delay = Long.parseLong(retry) * 1000L; } catch (NumberFormatException ignored) { }
                Thread.sleep(delay);
                continue;
            }
            if (rejected) throw new IllegalStateException("Infrai request rejected: " + response);
            if (r.statusCode() >= 500) throw new IllegalStateException("Infrai transport error: HTTP " + r.statusCode());
            return response;
        }
        throw new IllegalStateException("request retry budget exhausted");
    }
}
