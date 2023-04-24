package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.support.caching.CacheFrontend;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PreDestroy;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisActive;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisSentinel;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisSentinelEventsActive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Component;

@Component
@RedisActive(true)
@RedisSentinel(true)
@RedisSentinelEventsActive(true)
public class SentinelTopologyRefreshManager {

    private static final Logger log = LoggerFactory.getLogger(SentinelTopologyRefreshManager.class);

    private static final String SWITCH_MASTER_EVENT = "+switch-master";

    private String currentMaster;

    private final RedisClient redisClient;
    private final LettuceCacheManager cacheManager;
    private final CacheFrontendManager cacheFrontendManager;

    private final List<StatefulRedisPubSubConnection<String, String>> pubSubConnections = new ArrayList<>();

    @Autowired
    public SentinelTopologyRefreshManager(RedisClient redisClient, RedisURI redisURI,
            LettuceCacheManager cacheManager, CacheFrontendManager cacheFrontendManager) {
        this.redisClient = redisClient;
        this.cacheManager = cacheManager;
        this.cacheFrontendManager = cacheFrontendManager;

        this.getMasterIp().ifPresent(ip -> {
            this.currentMaster = ip;
            log.info("CURRENT master node '{}'", this.currentMaster);
        });

        for (RedisURI sentinelUri : redisURI.getSentinels()) {
            subscribeSentinel(sentinelUri);
        }

        log.debug("{} initialized", SentinelTopologyRefreshManager.class.getName());
    }

    private void subscribeSentinel(RedisURI sentinelUri) {
        log.debug("Subscribing to {} event on {}", SWITCH_MASTER_EVENT, sentinelUri);
        StatefulRedisPubSubConnection<String, String> pubSubConnection = redisClient.connectPubSub(sentinelUri);
        pubSubConnection.addListener(getSwitchMasterListener());
        pubSubConnection.sync().psubscribe(SWITCH_MASTER_EVENT);
        pubSubConnections.add(pubSubConnection);
    }

    @PreDestroy
    public void close() {
        for (StatefulRedisPubSubConnection<String, String> pubSubConnection : pubSubConnections) {
            pubSubConnection.close();
        }
    }

    private RedisPubSubAdapter<String, String> getSwitchMasterListener() {
        return new RedisPubSubAdapter<>() {
            @Override
            public void message(String pattern, String channel, String message) {
                log.debug("pattern: {}, channel: {}, message: {}", pattern, channel, message);
                String[] parts = message.split(" ");
                if (parts.length != 5) {
                    log.error("Unexpected message format for Sentinel switch master event: '{}'. "
                            + "Front-end-cache will not be refreshed. Consider switching to "
                            + "sentinel scheduler setting REDIS_USE_SENTINEL_EVENTS to false", message);
                    return;
                }
                String newMaster = parts[3];
                SentinelTopologyRefreshManager.this.updateMaster(newMaster);
            }
        };
    }

    private synchronized void updateMaster(String newMaster) {
        if (!newMaster.equals(currentMaster)) {
            log.warn("Refresh of front-end-cache -> from master node '{}' to '{}'",
                    SentinelTopologyRefreshManager.this.currentMaster, newMaster);
            rebuildCacheFrontend();
            this.currentMaster = newMaster;
        }
    }

    private void rebuildCacheFrontend() {
        CacheFrontend<String, Object> cacheFrontend = this.cacheFrontendManager.rebuildCacheFrontend();
        this.cacheManager.updateCacheFrontend(cacheFrontend);
        Collection<String> cacheNames = this.cacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            Cache cache = this.cacheManager.getCache(cacheName);
            if (cache instanceof LettuceCache) {
                ((LettuceCache) cache).setFrontendCache(cacheFrontend);
            }
        }
    }

    private Optional<String> getMasterIp() {
        log.debug("Checking Sentinel master IP");
        try (StatefulRedisSentinelConnection<String, String> connection = redisClient.connectSentinel()) {
            List<Map<String, String>> masters = connection.sync().masters();
            if (masters.isEmpty()) {
                log.warn("Sentinel master node not found!");
                return Optional.empty();
            }
            return Optional.of(masters.get(0).get("ip"));
        }
    }
}
