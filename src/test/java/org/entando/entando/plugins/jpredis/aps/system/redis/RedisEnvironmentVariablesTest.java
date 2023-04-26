package org.entando.entando.plugins.jpredis.aps.system.redis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RedisEnvironmentVariablesTest {

    @Test
    void testActive() {
        Assertions.assertTrue(RedisEnvironmentVariables.active());
    }

    @Test
    void testSessionActive() {
        Assertions.assertFalse(RedisEnvironmentVariables.redisSessionActive());
    }

    @Test
    void testSentinelActive() {
        Assertions.assertFalse(RedisEnvironmentVariables.sentinelActive());
    }

    @Test
    void testRedisAddress() {
        Assertions.assertEquals("redis://localhost:6380", RedisEnvironmentVariables.redisAddress());
    }

    @Test
    void testRedisAddresses() {
        Assertions.assertEquals("", RedisEnvironmentVariables.redisAddresses());
    }

    @Test
    void testRedisPassword() {
        Assertions.assertEquals("", RedisEnvironmentVariables.redisPassword());
    }

    @Test
    void testRedisMasterName() {
        Assertions.assertEquals("mymaster", RedisEnvironmentVariables.redisMasterName());
    }

    @Test
    void testFrontEndCacheCheckDelay() {
        Assertions.assertEquals(30, RedisEnvironmentVariables.frontEndCacheCheckDelay());
    }

    @Test
    void testUseSentinelEvents() {
        Assertions.assertTrue(RedisEnvironmentVariables.useSentinelEvents());
    }
}
