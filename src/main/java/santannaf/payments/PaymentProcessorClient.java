package santannaf.payments;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

public final class PaymentProcessorClient {
    public static final PaymentProcessorClient INSTANCE = new PaymentProcessorClient();

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json";

    private final URI defaultURI;
    private final URI fallbackURI;
    private final HttpClient httpClient = HttpClientConfig.httpClient();

    private final long backoff;

    public static PaymentProcessorClient getInstance() {
        return INSTANCE;
    }

    private PaymentProcessorClient() {
        var defaultUrl = System.getenv("HOST_PROCESSOR_DEFAULT");
        if (defaultUrl == null || defaultUrl.isBlank()) {
            defaultUrl = "http://localhost:8001";
        }
        defaultUrl = defaultUrl.concat("/payments");

        var fallbackUrl = System.getenv("HOST_PROCESSOR_FALLBACK");
        if (fallbackUrl == null || fallbackUrl.isBlank()) {
            fallbackUrl = "http://localhost:8002";
        }
        fallbackUrl = fallbackUrl.concat("/payments");

        System.out.println("======================================");
        System.out.println("============ Loading URLs ============");
        System.out.println("======================================");
        System.out.println("default: " + defaultUrl);
        System.out.println("fallback: " + fallbackUrl);
        System.out.println("======================================");

        int n = Integer.parseInt(System.getenv().getOrDefault("NUM_BACK_OFF", "250"));
        backoff = n * 1_000_000L;

        defaultURI = URI.create(defaultUrl);
        fallbackURI = URI.create(fallbackUrl);
    }

    public boolean processPayment(PaymentRequest paymentRequest) {
        var request = buildRequest(paymentRequest.getPostData(), defaultURI);

        for (int i = 0; i < 15; i++) {
            if (sendPayment(request)) {
                return true;
            }

            LockSupport.parkNanos(this.backoff);
        }

        return false;
    }

    private boolean sendPayment(HttpRequest request) {
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int sc = response.statusCode();
            return sc == 200;
        } catch (Exception _) {
            return false;
        }
    }

    private HttpRequest buildRequest(byte[] body, URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .header(CONTENT_TYPE, CONTENT_TYPE_VALUE)
                .timeout(Duration.ofMillis(999))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
    }

    public boolean isUp() {
        try {
            var probe = HttpRequest.newBuilder()
                    .uri(defaultURI)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofMillis(200))
                    .build();
            var resp = httpClient.send(probe, HttpResponse.BodyHandlers.discarding());
            int sc = resp.statusCode();
            return sc < 500; // 405/404 ainda contam como "up" para o nosso objetivo
        } catch (Exception _) {
            return false;
        }
    }
}
