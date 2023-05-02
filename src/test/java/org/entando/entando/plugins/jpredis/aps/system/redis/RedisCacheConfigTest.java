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

import static org.entando.entando.plugins.jpredis.utils.RedisSentinelTestExtension.REDIS_SENTINEL_SERVICE;
import static org.entando.entando.plugins.jpredis.utils.RedisSentinelTestExtension.REDIS_SERVICE;
import static org.entando.entando.plugins.jpredis.utils.RedisSentinelTestExtension.REDIS_SLAVE_SERVICE;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.DefaultClientResources;
import java.util.Collections;
import org.entando.entando.TestEntandoJndiUtils;
import org.entando.entando.plugins.jpredis.utils.RedisSentinelTestExtension;
import org.entando.entando.plugins.jpredis.utils.RedisSentinelTestExtension.ServicePort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author E.Santoboni
 */
@ExtendWith(RedisSentinelTestExtension.class)
class RedisCacheConfigTest {

    @BeforeAll
    static void setUp() {
        TestEntandoJndiUtils.setupJndi();
    }

    @Test
    void testSingleNodeConfig(@ServicePort(REDIS_SERVICE) int redisPort) throws Exception {
        SingleNodeConfig config = new SingleNodeConfig();
        ReflectionTestUtils.setField(config, "redisAddress", "redis://localhost:" + redisPort);
        LettuceConnectionFactory factory = config.connectionFactory();
        Assertions.assertNotNull(factory);
    }

    @Test
    void testSentinelConfigMasterSlavePorts(@ServicePort(REDIS_SERVICE) int redisPort,
            @ServicePort(REDIS_SLAVE_SERVICE) int redisSlavePort) throws Exception {
        SentinelConfig config = new SentinelConfig();
        ReflectionTestUtils.setField(config, "redisAddresses",
                "redis://localhost:" + redisPort + ",redis://localhost:" + redisSlavePort + ",redis://localhost:6382");
        LettuceConnectionFactory factory = config.connectionFactory();
        Assertions.assertNotNull(factory);
    }

    @Test
    void testSentinelConfigSentinelPort(@ServicePort(REDIS_SENTINEL_SERVICE) int redisSentinelPort) throws Exception {
        SentinelConfig config = new SentinelConfig();
        ReflectionTestUtils.setField(config, "redisAddresses",
                String.join(",", Collections.nCopies(3, "localhost:" + redisSentinelPort)));
        ReflectionTestUtils.setField(config, "redisMasterName", "redis");
        LettuceConnectionFactory factory = config.connectionFactory();
        Assertions.assertNotNull(factory);
        RedisURI redisURI = config.redisURI();
        RedisClient client = config.getRedisClient(DefaultClientResources.builder().build(), redisURI);
        Assertions.assertNotNull(client);
    }

}
