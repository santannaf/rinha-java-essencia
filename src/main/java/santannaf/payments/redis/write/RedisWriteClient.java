package santannaf.payments.redis.write;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class RedisWriteClient {

    private static final RedisWriteClient INSTANCE = new RedisWriteClient();

    public final JedisPool pool;

    private RedisWriteClient() {
        JedisPoolConfig cfg = new JedisPoolConfig();

        String redisHost = System.getenv("REDIS_HOST");
        if (redisHost == null || redisHost.isBlank()) {
            redisHost = "localhost";
        }

        int n = Integer.parseInt(System.getenv().getOrDefault("NUM_WORKERS_WRITE", "10"));

//        cfg.setMaxTotal(Math.max(2 * n, 32));
        cfg.setMaxTotal(n);
        cfg.setMaxIdle(n);
//        cfg.setMinIdle(Math.min(8, n));
        cfg.setMinIdle(n);

        cfg.setBlockWhenExhausted(true);
        cfg.setJmxEnabled(false);
        cfg.setTestOnBorrow(false);
        cfg.setTestOnReturn(false);
        cfg.setTestWhileIdle(false);

        this.pool = new JedisPool(cfg, redisHost, 6379, 200);
    }

    public static RedisWriteClient getInstance() {
        return INSTANCE;
    }
}
