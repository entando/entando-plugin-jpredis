package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisActive;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisSentinel;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisSentinelEventsActive;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RedisActive(true)
@RedisSentinel(true)
@RedisSentinelEventsActive(false)
public class SentinelSchedulerConfig {

    private final int frontEndCacheCheckDelay;

    public SentinelSchedulerConfig() {
        this.frontEndCacheCheckDelay = RedisEnvironmentVariables.frontEndCacheCheckDelay();
    }

    @Bean
    public Executor sentinelSchedulerExecutor(RedisClient redisClient, LettuceCacheManager cacheManager,
            CacheFrontendManager cacheFrontendManager) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        SentinelScheduler sentinelScheduler = new SentinelScheduler(redisClient, cacheManager, cacheFrontendManager);
        executor.scheduleWithFixedDelay(sentinelScheduler, frontEndCacheCheckDelay, frontEndCacheCheckDelay,
                TimeUnit.SECONDS);
        return executor;
    }
}
