package santannaf.payments;

import redis.clients.jedis.Jedis;
import santannaf.payments.redis.write.RedisWriteClient;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public final class ProcessWorker {
    private final static ProcessWorker INSTANCE = new ProcessWorker();

    private final PaymentProcessorClient client = PaymentProcessorClient.getInstance();
    private final RedisWriteClient redisClient = RedisWriteClient.getInstance();
    private final Semaphore semaphore = new Semaphore(0);
    private final ConcurrentLinkedQueue<PaymentRequest> queue = new ConcurrentLinkedQueue<>();
    private final Integer workers;

    private static final byte[] KEY_PAYMENTS = "payments".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    private static final int FAILS_TO_PAUSE = Integer.parseInt(System.getenv().getOrDefault("FAILS_TO_PAUSE", "3"));
    private static final long PROBE_INTERVAL_MS = Long.parseLong(System.getenv().getOrDefault("PROBE_INTERVAL_MS", "100")) * 1_000_000L;

    private final AtomicInteger failStreak = new AtomicInteger(0);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private ProcessWorker() {
        String workers = System.getenv("NUM_WORKERS");
        if (workers == null || workers.isEmpty()) {
            workers = "10";
        }

        this.workers = Integer.valueOf(workers);
        System.out.println("Worker count: " + workers);

        Thread.startVirtualThread(this::init);
    }

    public static ProcessWorker getInstance() {
        return INSTANCE;
    }

    public void init() {
        for (int i = 0; i < workers; i++) {
            Thread.startVirtualThread(() -> {
                try (Jedis jedis = redisClient.pool.getResource()) {
                    start(jedis);
                } catch (Exception _) {
                }
            });
        }
    }

    private void start(Jedis jedis) {
        for (; ; ) {
            try {
                if (paused.get()) {
                    if (client.isUp()) { // probe leve
                        failStreak.set(0);
                        paused.set(false);
                    } else {
                        LockSupport.parkNanos(PROBE_INTERVAL_MS);
                    }
                    continue;
                }

                semaphore.acquire();
                PaymentRequest pr = queue.poll();
                if (pr == null) { // corrida entre workers
                    semaphore.release();
                    continue;
                }

                processPayment(jedis, pr);
            } catch (Exception _) {
//                e.printStackTrace();
            }
        }
    }

    void processPayment(Jedis jedis, PaymentRequest pr) {
        if (client.processPayment(pr)) {
            failStreak.set(0);
            var prr = pr.parseToDefault();
            final double score = prr.requestedAt;
            final byte[] member = DataBuilder.buildBytes(prr);
            jedis.zadd(KEY_PAYMENTS, score, member);
        } else {
            queue.offer(pr);
            semaphore.release();
            if (failStreak.incrementAndGet() >= FAILS_TO_PAUSE) {
                paused.set(true);
            }
        }
    }

    public void producer(byte[] paymentRequest) {
        queue.offer(new PaymentRequest(paymentRequest));
        semaphore.release();
    }
}
