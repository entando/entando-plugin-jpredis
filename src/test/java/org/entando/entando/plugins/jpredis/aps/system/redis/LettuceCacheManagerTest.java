package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.support.caching.CacheFrontend;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

@ExtendWith(MockitoExtension.class)
class LettuceCacheManagerTest {

    @Mock
    private RedisCacheWriter redisCacheWriter;
    @Mock
    private RedisCacheConfiguration redisCacheConfiguration;
    @Mock
    private CacheFrontend<String, Object> cacheFrontend;

    private LettuceCacheManager lettuceCacheManager;

    @BeforeEach
    void setUp() {
        lettuceCacheManager = new LettuceCacheManager(redisCacheWriter, redisCacheConfiguration,
                new HashMap<>(), false, cacheFrontend);
    }

    @Test
    void shouldUpdateCacheFrontend() {
        Mockito.when(cacheFrontend.get("test::testKey1")).thenReturn("testValue1");
        RedisCache cache1 = lettuceCacheManager.createRedisCache("test", null);
        Assertions.assertEquals("testValue1", cache1.get("testKey1").get());

        CacheFrontend<String, Object> cacheFrontend = Mockito.mock(CacheFrontend.class);
        Mockito.when(cacheFrontend.get("test::testKey1")).thenReturn(null);
        Mockito.when(cacheFrontend.get("test::testKey2")).thenReturn("testValue2");
        lettuceCacheManager.updateCacheFrontend(cacheFrontend);
        RedisCache cache2 = lettuceCacheManager.createRedisCache("test", null);

        Assertions.assertNull(cache2.get("testKey1"));
        Assertions.assertEquals("testValue2", cache2.get("testKey2").get());
    }
}
