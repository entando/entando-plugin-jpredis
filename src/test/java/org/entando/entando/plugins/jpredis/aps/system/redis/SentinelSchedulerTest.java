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

import static org.mockito.Mockito.when;

import io.lettuce.core.RedisClient;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author E.Santoboni
 */
@ExtendWith(MockitoExtension.class)
class SentinelSchedulerTest {
    
    @Mock
    private Map<String, String> master;
    
    @Mock
    private RedisSentinelCommands commands;
    
    @Mock
    private RedisClient lettuceClient;
    
	@Mock
    private CacheConfig cacheConfig;
    
    @BeforeEach
    void setUp() throws Exception {
        StatefulRedisSentinelConnection connection = Mockito.mock(StatefulRedisSentinelConnection.class);
        when(this.lettuceClient.connectSentinel()).thenReturn(connection);
        when(connection.sync()).thenReturn(this.commands);
        List<Map<String, String>> masters = Arrays.asList(master);
        when(commands.masters()).thenReturn(masters);
        Mockito.lenient().when(master.get("ip")).thenReturn("myMaster").thenReturn("mySecondMaster");
    }
    
    @Test
    void getInitScheduler() throws Exception {
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, 1, cacheConfig);
        Mockito.verify(master, Mockito.times(1)).get("ip");
    }
    
    @Test
    void runScheduler_1() throws Exception {
         when(master.get("ip")).thenReturn("myMaster");
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, 1, cacheConfig);
        scheduler.run();
        Mockito.verify(master, Mockito.times(2)).get("ip");
        Mockito.verify(cacheConfig, Mockito.times(0)).rebuildCacheFrontend(this.lettuceClient);
    }
    
    @Test
    void runScheduler_2() throws Exception {
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, 1, cacheConfig);
        scheduler.run();
        Mockito.verify(master, Mockito.times(2)).get("ip");
        Mockito.verify(cacheConfig, Mockito.times(1)).rebuildCacheFrontend(this.lettuceClient);
        scheduler.cancel();
    }
    
    @Test
    void runScheduler_3() throws Exception {
        when(commands.masters()).thenReturn(new ArrayList());
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, 1, cacheConfig);
        scheduler.run();
        Mockito.verify(master, Mockito.times(0)).get("ip");
        Mockito.verify(cacheConfig, Mockito.times(0)).rebuildCacheFrontend(this.lettuceClient);
    }
    
    @Test
    void runScheduler_4() throws Exception {
        CacheConfig config = new CacheConfig();
        CacheManager mockCacheManager = Mockito.mock(CacheManager.class);
        ReflectionTestUtils.setField(config, "cacheManagerBean", mockCacheManager);
        when(mockCacheManager.getCacheNames()).thenReturn(Arrays.asList("cache1", "cache2"));
        LettuceCache cache = Mockito.mock(LettuceCache.class);
        when(mockCacheManager.getCache(Mockito.anyString())).thenReturn(cache);
        StatefulRedisConnectionImpl connection = Mockito.mock(StatefulRedisConnectionImpl.class);
        when(lettuceClient.connect(Mockito.any(RedisCodec.class))).thenReturn(connection);
        RedisCommands mockCommands = Mockito.mock(RedisCommands.class);
        when(connection.sync()).thenReturn(mockCommands);
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, 1, config);
        ReflectionTestUtils.setField(config, "scheduler", scheduler);
        scheduler.run();
        Mockito.verify(master, Mockito.times(2)).get("ip");
        Mockito.verify(cache, Mockito.times(2)).setFrontendCache(Mockito.any());
        scheduler.cancel();
    }
    
    @Test
    void failRunScheduler() throws Exception {
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, 1, cacheConfig);
        Assertions.assertThrows(EntRuntimeException.class, () -> {
            Mockito.doThrow(RuntimeException.class).when(this.cacheConfig).rebuildCacheFrontend(this.lettuceClient);
            try {
                scheduler.run();
            } catch (RuntimeException e) {
                throw e;
            } finally {
                Mockito.verify(master, Mockito.times(2)).get("ip");
            }
        });
    }
    
}
