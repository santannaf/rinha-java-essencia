package santannaf.payments;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class PaymentsSummaryHandler implements HttpHandler {
    private final PaymentsRepository paymentsRepository;

    PaymentsSummaryHandler() {
        this.paymentsRepository = PaymentsRepository.getInstance();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        final URI uri = ex.getRequestURI();
        final String q = uri.getRawQuery();
        final String fromStr = getQuery(q, "from");
        final String toStr = getQuery(q, "to");

        final Instant now = Instant.now();
        Instant from;
        Instant to;

        if (fromStr == null || fromStr.isBlank()) {
            // intervalo mínimo absoluto
            from = Instant.ofEpochMilli(Long.MIN_VALUE);
        } else {
            from = parseInstantSafe(fromStr);
        }

        if (toStr == null || toStr.isBlank()) {
            to = now;
        } else {
            to = parseInstantSafe(toStr);
        }

        // valida parsing e ordem do intervalo
        if (from == null || to == null || to.isBefore(from)) {
            ex.sendResponseHeaders(400, 0);
            ex.close();
            return;
        }

        final long fromMs = from.toEpochMilli();
        final long toMs = to.toEpochMilli();

        var s = paymentsRepository.range(fromMs, toMs);

        final byte[] out = s.toJson().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(200, out.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(out);
        }
    }

    private static Instant parseInstantSafe(String s) {
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static String getQuery(String raw, String key) {
        if (raw == null) return null;
        int p = raw.indexOf(key + "=");
        if (p < 0) return null;
        int s = p + key.length() + 1, e = raw.indexOf('&', s);
        String v = (e < 0) ? raw.substring(s) : raw.substring(s, e);
        return java.net.URLDecoder.decode(v, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }
}
