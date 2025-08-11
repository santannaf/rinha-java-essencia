package santannaf.payments;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import santannaf.payments.redis.read.RedisReadClient;

import java.math.BigDecimal;
import java.util.List;

public final class PaymentsRepository {
    public static final PaymentsRepository INSTANCE = new PaymentsRepository();

    private final JedisPool pool;

    private PaymentsRepository() {
        this.pool = RedisReadClient.getInstance().pool;
    }

    public static PaymentsRepository getInstance() {
        return INSTANCE;
    }

    public PaymentSummary range(long fromMs, long toMs) {
        try (Jedis j = this.pool.getResource()) {
            List<String> defBuckets = j.zrangeByScore("payments", fromMs, toMs);

            long defaultCount = 0;
            long fallbackCount = 0;
            long defaultAmount = 0L;
            long fallbackAmount = 0L;

            for (String payment : defBuckets) {
                String[] parts = payment.split(":");
                long amount = Long.parseLong(parts[1]);
                boolean isDefault = parts[2].equals("1");

                if (isDefault) {
                    defaultCount++;
                    defaultAmount += amount;
                } else {
                    fallbackCount++;
                    fallbackAmount += amount;
                }
            }

            return new PaymentSummary(
                    new PaymentSummary.Summary(defaultCount, new BigDecimal(defaultAmount).movePointLeft(2)),
                    new PaymentSummary.Summary(fallbackCount, new BigDecimal(fallbackAmount).movePointLeft(2))
            );
        }
    }
}
