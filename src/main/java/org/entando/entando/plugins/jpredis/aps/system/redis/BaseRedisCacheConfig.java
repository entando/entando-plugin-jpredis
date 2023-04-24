/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.support.caching.CacheFrontend;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.entando.entando.aps.system.services.cache.ICacheInfoManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * @author E.Santoboni
 */
public class BaseRedisCacheConfig extends CachingConfigurerSupport {

    protected static final String REDIS_PREFIX = "redis://";

    @Bean(destroyMethod = "shutdown")
    public DefaultClientResources defaultClientResources() {
        return DefaultClientResources.builder().build();
    }

    @Primary
    @Bean
    public CacheManager cacheManager(RedisClient redisClient,
            RedisConnectionFactory redisConnectionFactory, CacheFrontendManager cacheFrontendManager) {
        RedisCacheConfiguration redisCacheConfiguration = this.buildDefaultConfiguration();
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        long ttlInSeconds = TimeUnit.HOURS.toSeconds(4);

        cacheConfigurations.put(ICacheInfoManager.DEFAULT_CACHE_NAME, createCacheConfiguration(ttlInSeconds));

        CacheFrontend<String, Object> cacheFrontend = cacheFrontendManager.getCacheFrontend();
        return LettuceCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration).cacheFrontend(cacheFrontend)
                .withInitialCacheConfigurations(cacheConfigurations).build();
    }

    private static RedisCacheConfiguration createCacheConfiguration(long ttlInSeconds) {
        return RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(ttlInSeconds));
    }

    private RedisCacheConfiguration buildDefaultConfiguration() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ZERO);
        ObjectMapper mapper = new Jackson2ObjectMapperBuilder().failOnEmptyBeans(false).build();
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        config = config.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer()));
        return config;
    }

}
