package santannaf.payments;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import santannaf.payments.redis.write.RedisWriteClient;

public final class Warmup {

    public static final Warmup INSTANCE = new Warmup();

    public static Warmup getInstance() {
        return INSTANCE;
    }

    private Warmup() {
        var redisClient = RedisWriteClient.getInstance();
        long now = System.currentTimeMillis();
        long b = (now / 60_000L) * 60_000L;
        try (Jedis j = redisClient.pool.getResource()) {
            Pipeline p = j.pipelined();
            p.incrBy("p:cnt:default:" + b, 0);
            p.incrByFloat("p:sum:default:" + b, 0.0);
            p.sync();
        }
    }
}
