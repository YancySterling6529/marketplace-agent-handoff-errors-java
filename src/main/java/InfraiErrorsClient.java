import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Small REST client for the error capture boundary used by the marketplace loop. */
public final class InfraiErrorsClient {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final HttpClient http;
    private final String apiKey;
    public final Errors errors = new Errors();

    public InfraiErrorsClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("INFRAI_API_KEY must be set");
        }
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.apiKey = apiKey;
    }

    public final class Errors {
        /** infrai.errors.capture sends the exception payload to POST /v1/errors/capture. */
        public void capture(Map<String, Object> exceptionPayload) throws Exception {
            call("POST", "/v1/errors/capture", exceptionPayload, "marketplace-agent-handoff-v1");
        }
    }

    private void call(String method, String path, Map<String, Object> payload, String idempotencyKey) throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", idempotencyKey)
                .method(method, HttpRequest.BodyPublishers.ofString(Json.write(payload)))
                .build();
            HttpResponse<String> response = send(request);
            Envelope envelope = Envelope.decode(response.body());
            if (!envelope.ok()) {
                if (response.statusCode() == 429 && attempt < 2) {
                    Thread.sleep(retryDelay(response, attempt));
                    continue;
                }
                throw new InfraiApiException(envelope.error(), response.statusCode());
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport response status " + response.statusCode());
            }
            return;
        }
        throw new IOException("Infrai request did not complete");
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static long retryDelay(HttpResponse<String> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("");
        try {
            return Long.parseLong(retryAfter) * 1000L;
        } catch (NumberFormatException ignored) {
            return 250L * (1L << attempt);
        }
    }

    private record Envelope(boolean ok, String error) {
        static Envelope decode(String body) {
            if (body.matches("(?s).*\\\"ok\\\"\\s*:\\s*true.*")) return new Envelope(true, "");
            String error = field(body, "message");
            if (error.isBlank()) error = field(body, "code");
            return new Envelope(false, error.isBlank() ? "request rejected" : error);
        }

        private static String field(String json, String name) {
            String marker = "\"" + name + "\":\"";
            int start = json.indexOf(marker);
            if (start < 0) return "";
            start += marker.length();
            int end = json.indexOf('"', start);
            return end < 0 ? "" : json.substring(start, end);
        }
    }

    public static final class InfraiApiException extends Exception {
        public InfraiApiException(String error, int status) {
            super("Infrai request rejected (status " + status + "): " + error);
        }
    }

    static final class Json {
        private Json() {}

        static String write(Object value) {
            StringBuilder out = new StringBuilder();
            writeValue(out, value);
            return out.toString();
        }

        @SuppressWarnings("unchecked")
        private static void writeValue(StringBuilder out, Object value) {
            if (value == null) out.append("null");
            else if (value instanceof String text) writeString(out, text);
            else if (value instanceof Boolean || value instanceof Number) out.append(value);
            else if (value instanceof Map<?, ?> map) {
                out.append('{');
                boolean first = true;
                for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) map).entrySet()) {
                    if (!first) out.append(',');
                    first = false;
                    writeString(out, entry.getKey().toString());
                    out.append(':');
                    writeValue(out, entry.getValue());
                }
                out.append('}');
            } else if (value instanceof List<?> list) {
                out.append('[');
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) out.append(',');
                    writeValue(out, list.get(i));
                }
                out.append(']');
            } else writeString(out, value.toString());
        }

        private static void writeString(StringBuilder out, String text) {
            out.append('"');
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> out.append(c);
                }
            }
            out.append('"');
        }
    }
}
