package santannaf.payments.redis.read;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisReadClient {
    private static final RedisReadClient INSTANCE = new RedisReadClient();

    public final JedisPool pool;

    private RedisReadClient() {
        JedisPoolConfig cfg = new JedisPoolConfig();

        String redisHost = System.getenv("REDIS_HOST");
        if (redisHost == null || redisHost.isBlank()) {
            redisHost = "localhost";
        }

        int n = Integer.parseInt(System.getenv().getOrDefault("NUM_WORKERS_READ", "10"));

        cfg.setMaxTotal(Math.max(2 * n, 32));
        cfg.setMaxIdle(n);
        cfg.setMinIdle(Math.min(8, n));

        cfg.setBlockWhenExhausted(true);
        cfg.setJmxEnabled(false);
        cfg.setTestOnBorrow(false);
        cfg.setTestOnReturn(false);
        cfg.setTestWhileIdle(false);

        this.pool = new JedisPool(cfg, redisHost, 6379, 200);
    }

    public static RedisReadClient getInstance() {
        return INSTANCE;
    }
}
