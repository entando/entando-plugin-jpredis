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
import io.lettuce.core.RedisURI;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.cache.ExternalCachesContainer;
import org.entando.entando.aps.system.services.cache.ICacheInfoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * @author E.Santoboni
 */
@Configuration
@ComponentScan
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport {

    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    private static final String REDIS_PREFIX = "redis://";

    @Value("${REDIS_ACTIVE:false}")
    private boolean active;

    @Value("${REDIS_ADDRESS:redis://localhost:6379}")
    private String redisAddress;

    @Value("${REDIS_ADDRESSES:}")
    private String redisAddresses;

    @Value("${REDIS_MASTER_NAME:mymaster}")
    private String redisMasterName;

    @Value("${REDIS_PASSWORD:}")
    private String redisPassword;
    
    @Autowired
    @Qualifier(value = "entandoDefaultCaches")
    private Collection<Cache> defaultCaches;
    
    @Autowired(required = false)
    private List<ExternalCachesContainer> defaultExternalCachesContainers;

    private CacheManager cacheManagerBean;

    private static RedisCacheConfiguration createCacheConfiguration(long timeoutInSeconds) {
        return RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(timeoutInSeconds));
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        if (!this.active) {
            return new LettuceConnectionFactory();
        }
        if (!StringUtils.isBlank(this.redisAddresses)) {
            logger.warn("** Redis Cluster with sentinel configuration - the master node will be the first node defined in REDIS_ADDRESSES parameter **");
            String[] addresses = this.redisAddresses.split(",");
            RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
            for (int i = 0; i < addresses.length; i++) {
                String address = addresses[i];
                String purgedAddress = (address.trim().startsWith(REDIS_PREFIX)) ? address.trim().substring(REDIS_PREFIX.length()) : address.trim();
                String[] sections = purgedAddress.split(":");
                RedisNode node = new RedisNode(sections[0], Integer.parseInt(sections[1]));
                if (i == 0) {
                    node.setName(this.redisMasterName);
                    sentinelConfig.setMaster(node);
                } else {
                    sentinelConfig.addSentinel(node);
                }
            }
            if (!StringUtils.isBlank(this.redisPassword)) {
                sentinelConfig.setPassword(this.redisPassword);
            }
            return new LettuceConnectionFactory(sentinelConfig);
        } else {
            logger.info("** Redis with single node configuration **");
            RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
            String[] sections = this.redisAddress.substring(REDIS_PREFIX.length()).split(":");
            redisStandaloneConfiguration.setHostName(sections[0]);
            redisStandaloneConfiguration.setPort(Integer.parseInt(sections[1]));
            if (!StringUtils.isBlank(this.redisPassword)) {
                redisStandaloneConfiguration.setPassword(this.redisPassword);
            }
            return new LettuceConnectionFactory(redisStandaloneConfiguration);
        }
    }

    @Primary
    @Bean
    public CacheManager cacheManager(@Autowired(required = false) RedisClient lettuceClient, RedisConnectionFactory redisConnectionFactory) {
        if (!this.active) {
            logger.warn("** Redis not active **");
            DefaultEntandoCacheManager defaultCacheManager = new DefaultEntandoCacheManager();
            defaultCacheManager.setCaches(this.defaultCaches);
            defaultCacheManager.setExternalCachesContainers(this.getDefaultExternalCachesContainers());
            defaultCacheManager.afterPropertiesSet();
            return defaultCacheManager;
        }
        RedisCacheConfiguration redisCacheConfiguration = this.buildDefaultConfiguration();
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // time to leave = 4 Hours
        cacheConfigurations.put(ICacheInfoManager.DEFAULT_CACHE_NAME, createCacheConfiguration(4L * 60 * 60));
        CacheFrontend<String, Object> cacheFrontend = this.buildCacheFrontend(lettuceClient);
        LettuceCacheManager manager = LettuceCacheManager
                .builder(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .cacheFrontend(cacheFrontend)
                .withInitialCacheConfigurations(cacheConfigurations).build();
        this.setCacheManagerBean(manager);
        return manager;
    }

    @Bean
    public RedisClient getRedisClient() {
        if (!this.active) {
            return null;
        }
        DefaultClientResources resources = DefaultClientResources.builder().build();
        RedisClient lettuceClient = null;
        if (!StringUtils.isBlank(this.redisAddresses)) {
            logger.warn("** Client-side caching doesn't work on Redis Cluster and sharding data environments but only for Master/Slave environments (with sentinel) **");
            List<String> purgedAddresses = new ArrayList<>();
            String[] addresses = this.redisAddresses.split(",");
            for (int i = 0; i < addresses.length; i++) {
                String address = addresses[i];
                if (!address.trim().startsWith(REDIS_PREFIX)) {
                    purgedAddresses.add(address.trim());
                } else {
                    purgedAddresses.add(address.trim().substring(REDIS_PREFIX.length()));
                }
            }
            String[] sectionForMaster = purgedAddresses.get(0).split(":");
            RedisURI.Builder uriBuilder = RedisURI.Builder.sentinel(sectionForMaster[0], Integer.parseInt(sectionForMaster[1]), this.redisMasterName);
            if (addresses.length > 1) {
                for (int i = 1; i < purgedAddresses.size(); i++) {
                    String[] sectionForSlave = purgedAddresses.get(i).split(":");
                    uriBuilder.withSentinel(sectionForSlave[0], Integer.parseInt(sectionForSlave[1]));
                }
            }
            RedisURI redisUri = uriBuilder.build();
            if (!StringUtils.isBlank(this.redisPassword)) {
                redisUri.setPassword(this.redisPassword.toCharArray());
            }
            lettuceClient = new RedisClient(resources, redisUri) {};
        } else {
            RedisURI redisUri = RedisURI.create((this.redisAddress.startsWith(REDIS_PREFIX)) ? this.redisAddress : REDIS_PREFIX + this.redisAddress);
            if (!StringUtils.isBlank(this.redisPassword)) {
                redisUri.setPassword(this.redisPassword.toCharArray());
            }
            lettuceClient = new RedisClient(resources, redisUri) {};
        }
        return lettuceClient;
    }

    protected CacheFrontend<String, Object> buildCacheFrontend(RedisClient lettuceClient) {
        StatefulRedisConnection<String, Object> myself = lettuceClient.connect(new SerializedObjectCodec());
        TrackingArgs trackingArgs = TrackingArgs.Builder.enabled().bcast();
        Map<String, Object> clientCache = new ConcurrentHashMap<>();
        CacheFrontend<String, Object> cacheFrontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), myself, trackingArgs);
        return cacheFrontend;
    }
    
    private RedisCacheConfiguration buildDefaultConfiguration() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ZERO);
        ObjectMapper mapper = new Jackson2ObjectMapperBuilder()
                .failOnEmptyBeans(false)
                .build();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer()));
        return config;
    }

    protected CacheManager getCacheManagerBean() {
        return cacheManagerBean;
    }
    protected void setCacheManagerBean(CacheManager cacheManagerBean) {
        this.cacheManagerBean = cacheManagerBean;
    }

    protected List<ExternalCachesContainer> getDefaultExternalCachesContainers() {
        return defaultExternalCachesContainers;
    }
    protected void setDefaultExternalCachesContainers(List<ExternalCachesContainer> defaultExternalCachesContainers) {
        this.defaultExternalCachesContainers = defaultExternalCachesContainers;
    }

}
