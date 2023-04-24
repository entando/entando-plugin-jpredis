package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisActive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RedisActive(true)
public class CacheFrontendManager {

    private final RedisClient redisClient;
    private CacheFrontend<String, Object> cacheFrontend;

    @Autowired
    public CacheFrontendManager(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    public CacheFrontend<String, Object> getCacheFrontend() {
        if (this.cacheFrontend == null) {
            this.cacheFrontend = this.buildCacheFrontend();
        }
        return this.cacheFrontend;
    }

    public CacheFrontend<String, Object> rebuildCacheFrontend() {
        if (this.cacheFrontend != null) {
            this.cacheFrontend.close(); // this closes the underlying connection
        }
        this.cacheFrontend = this.buildCacheFrontend();
        return this.cacheFrontend;
    }

    private CacheFrontend<String, Object> buildCacheFrontend() {
        StatefulRedisConnection<String, Object> connection = redisClient.connect(new SerializedObjectCodec());
        TrackingArgs trackingArgs = TrackingArgs.Builder.enabled().bcast();
        Map<String, Object> clientCache = new ConcurrentHashMap<>();
        return ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection, trackingArgs);
    }
}
