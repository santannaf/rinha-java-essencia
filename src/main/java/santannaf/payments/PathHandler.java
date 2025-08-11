package santannaf.payments;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class PathHandler {
    private final HttpHandler handlers;

    public PathHandler() {
        this.handlers = configureHandlers();
    }

    private HttpHandler configureHandlers() {
        // Instâncias dedicadas (evita criar a cada request)
        final HttpHandler payments = new PaymentsHandler();
        final HttpHandler paymentsSummary = new PaymentsSummaryHandler();
        return new Router(payments, paymentsSummary);
    }

    public HttpHandler getHandlers() {
        return handlers;
    }

    // Router minimalista: switch por caminho exato
    private static final class Router implements HttpHandler {
        private final HttpHandler payments;
        private final HttpHandler paymentsSummary;

        Router(HttpHandler payments, HttpHandler paymentsSummary) {
            this.payments = payments;
            this.paymentsSummary = paymentsSummary;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // rawPath evita custo de decodificação (mantém %xx se houver)
            final String path = exchange.getRequestURI().getRawPath();
            switch (path) {
                case "/payments":
                    payments.handle(exchange);
                    return;
                case "/payments-summary":
                    paymentsSummary.handle(exchange);
                    return;
                default:
                    // 404 sem corpo (mínimo overhead)
                    exchange.sendResponseHeaders(404, 0);
                    exchange.close();
            }
        }
    }
}
