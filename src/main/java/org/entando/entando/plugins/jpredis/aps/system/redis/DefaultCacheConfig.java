package org.entando.entando.plugins.jpredis.aps.system.redis;

import java.util.Collection;
import java.util.List;
import org.entando.entando.aps.system.services.cache.ExternalCachesContainer;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisActive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableCaching
@RedisActive(false)
public class DefaultCacheConfig extends CachingConfigurerSupport {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCacheConfig.class);

    @Primary
    @Bean
    public CacheManager cacheManager(@Autowired @Qualifier(value = "entandoDefaultCaches") Collection<Cache> defaultCaches,
            @Autowired(required = false) List<ExternalCachesContainer> defaultExternalCachesContainers) {
        logger.warn("** Redis not active **");
        DefaultEntandoCacheManager defaultCacheManager = new DefaultEntandoCacheManager();
        defaultCacheManager.setCaches(defaultCaches);
        defaultCacheManager.setExternalCachesContainers(defaultExternalCachesContainers);
        defaultCacheManager.afterPropertiesSet();
        return defaultCacheManager;
    }
}
