/*
 * Copyright 2022-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import java.util.TimerTask;
import org.entando.entando.plugins.jpredis.RedisTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author E.Santoboni
 */
@ExtendWith(MockitoExtension.class)
class CacheConfigTest {
    
    @BeforeAll
    public static void startUp() throws Exception {
        RedisTestUtils.startContainer(true);
    }
    
    @AfterAll
    public static void tearDown() throws Exception {
        RedisTestUtils.stopContainer();
    }
    
    @Test
    void testRedisConnectionFactory_1() throws Exception {
        CacheConfig config = new CacheConfig();
        ReflectionTestUtils.setField(config, "active", true);
        ReflectionTestUtils.setField(config, "redisAddress", "redis://localhost:6380");
        LettuceConnectionFactory factory = config.redisConnectionFactory();
        Assertions.assertNotNull(factory);
        TimerTask scheduler = (TimerTask) ReflectionTestUtils.getField(config, "scheduler");
        Assertions.assertNull(scheduler);
    }

    @Test
    void testRedisConnectionFactory_2() throws Exception {
        CacheConfig config = new CacheConfig();
        ReflectionTestUtils.setField(config, "active", true);
        ReflectionTestUtils.setField(config, "redisAddresses", "redis://localhost:6380,redis://localhost:6381,redis://localhost:6382");
        LettuceConnectionFactory factory = config.redisConnectionFactory();
        Assertions.assertNotNull(factory);
        TimerTask scheduler = (TimerTask) ReflectionTestUtils.getField(config, "scheduler");
        Assertions.assertNull(scheduler);
    }

    @Test
    void testCacheManager() throws Exception {
        CacheConfig config = new CacheConfig();
        ReflectionTestUtils.setField(config, "active", true);
        ReflectionTestUtils.setField(config, "redisAddresses", "localhost:26379,localhost:26379,localhost:26379");
        ReflectionTestUtils.setField(config, "redisMasterName", "redis");
        LettuceConnectionFactory factory = config.redisConnectionFactory();
        Assertions.assertNotNull(factory);
        RedisClient client = config.getRedisClient();
        Assertions.assertNotNull(client);
        /*
        CacheManager cacheManager = config.cacheManager(client, factory);
        Assertions.assertNotNull(cacheManager);
        TimerTask scheduler = (TimerTask) ReflectionTestUtils.getField(config, "scheduler");
        Assertions.assertNotNull(scheduler);
        */
        config.destroy();
    }
    
}
