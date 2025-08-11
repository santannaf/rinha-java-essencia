package santannaf.payments;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class PaymentProcessorClient {

    public static final PaymentProcessorClient INSTANCE = new PaymentProcessorClient();

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json";

    private final URI defaultUri;
    private final HttpClient httpClient = HttpClientConfig.httpClient();

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

        defaultUri = URI.create(defaultUrl);
    }

    public boolean processPayment(PaymentRequest paymentRequest) {
        return postToDefault(paymentRequest);
    }

    private boolean postToDefault(PaymentRequest paymentRequest) {
        try {
            var response = httpClient.send(buildRequest(paymentRequest.getPostData(), defaultUri), HttpResponse.BodyHandlers.discarding());
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
}
