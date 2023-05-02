package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.DefaultClientResources;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisActive;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisSentinel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@EnableCaching
@RedisActive(true)
@RedisSentinel(true)
public class SentinelConfig extends BaseRedisCacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(SentinelConfig.class);

    private final String redisAddresses;
    private final String redisPassword;
    private final String redisMasterName;

    public SentinelConfig() {
        this.redisAddresses = RedisEnvironmentVariables.redisAddresses();
        this.redisPassword = RedisEnvironmentVariables.redisPassword();
        this.redisMasterName = RedisEnvironmentVariables.redisMasterName();
    }

    @Bean(destroyMethod = "destroy")
    public LettuceConnectionFactory connectionFactory() {
        logger.warn("** Redis Cluster with sentinel configuration **");
        String[] addresses = this.redisAddresses.split(",");
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
        for (int i = 0; i < addresses.length; i++) {
            String address = addresses[i];
            String purgedAddress =
                    (address.trim().startsWith(REDIS_PREFIX)) ? address.trim().substring(REDIS_PREFIX.length())
                            : address.trim();
            String[] sections = purgedAddress.split(":");
            RedisNode node = new RedisNode(sections[0], Integer.parseInt(sections[1]));
            sentinelConfig.addSentinel(node);
        }
        sentinelConfig.setMaster(this.redisMasterName);
        if (!StringUtils.isBlank(this.redisPassword)) {
            sentinelConfig.setPassword(this.redisPassword);
        }
        return new LettuceConnectionFactory(sentinelConfig);
    }

    @Bean
    public RedisURI redisURI() {
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
        RedisURI.Builder uriBuilder = RedisURI.builder();
        if (addresses.length > 1) {
            for (int i = 0; i < purgedAddresses.size(); i++) {
                String[] sections = purgedAddresses.get(i).split(":");
                uriBuilder.withSentinel(sections[0], Integer.parseInt(sections[1]));
            }
        }
        uriBuilder.withSentinelMasterId(this.redisMasterName);
        if (!StringUtils.isBlank(this.redisPassword)) {
            uriBuilder.withPassword(this.redisPassword.toCharArray());
        }
        return uriBuilder.build();
    }

    @Bean(destroyMethod = "shutdown")
    public RedisClient getRedisClient(DefaultClientResources resources, RedisURI redisURI) {
        logger.warn(
                "** Client-side caching doesn't work on Redis Cluster and sharding data environments but only for Master/Slave environments (with sentinel) **");
        return RedisClient.create(resources, redisURI);
    }

}
