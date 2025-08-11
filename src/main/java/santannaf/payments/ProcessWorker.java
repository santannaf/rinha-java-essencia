package santannaf.payments;

import redis.clients.jedis.Jedis;
import santannaf.payments.redis.write.RedisWriteClient;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public final class ProcessWorker {
    private final static ProcessWorker INSTANCE = new ProcessWorker();

    private final PaymentProcessorClient client = PaymentProcessorClient.getInstance();
    private final RedisWriteClient redisClient = RedisWriteClient.getInstance();
    private final Semaphore semaphore = new Semaphore(0);
    private final ConcurrentLinkedQueue<PaymentRequest> queue = new ConcurrentLinkedQueue<>();
    private final Integer workers;

    private static final byte[] KEY_PAYMENTS = "payments".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

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
//                    e.printStackTrace();
                }
            });
        }
    }

    private void start(Jedis jedis) {
        while (true) {
            try {
                semaphore.acquire();
                PaymentRequest pr = queue.poll();
                processPayment(jedis, pr);
            } catch (Exception _) {
//                e.printStackTrace();
            }
        }
    }


    void processPayment(Jedis jedis, PaymentRequest pr) {
        if (client.processPayment(pr)) {
            var prr = pr.parseToDefault();
            final double score = prr.requestedAt;
            final byte[] member = DataBuilder.buildBytes(prr);
            jedis.zadd(KEY_PAYMENTS, score, member);
            return;
        }

        producer(pr.requestData);
    }

    public void producer(byte[] paymentRequest) {
        queue.offer(new PaymentRequest(paymentRequest));
        semaphore.release();
    }
}
