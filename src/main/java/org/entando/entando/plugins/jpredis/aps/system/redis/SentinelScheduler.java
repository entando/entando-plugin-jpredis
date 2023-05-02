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
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.support.caching.CacheFrontend;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimerTask;
import java.util.function.Consumer;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

/**
 * @author E.Santoboni
 */
public class SentinelScheduler extends TimerTask {

    private static final Logger logger = LoggerFactory.getLogger(SentinelScheduler.class);

    private final RedisClient redisClient;
    private final LettuceCacheManager cacheManager;
    private final CacheFrontendManager cacheFrontendManager;

    private String currentMasterIp;

    private int masterCheckLogCount = 0;

    public SentinelScheduler(RedisClient redisClient, LettuceCacheManager cacheManager,
            CacheFrontendManager cacheFrontendManager) {
        this.redisClient = redisClient;
        this.cacheManager = cacheManager;
        this.cacheFrontendManager = cacheFrontendManager;
        this.getMasterIp().ifPresent(ip -> {
            this.currentMasterIp = ip;
            logger.info("CURRENT master node '{}'", this.currentMasterIp);
        });
    }

    @Override
    public void run() {
        try {
            this.getMasterIp().ifPresent(ip -> {
                if (!ip.equals(this.currentMasterIp)) {
                    logger.info("Refresh of front-end-cache -> from master node '{}' to '{}'",
                            this.currentMasterIp, ip);
                    this.rebuildCacheFrontend();
                    this.currentMasterIp = ip;
                }
            });
        } catch (Exception e) {
            throw new EntRuntimeException("Error on executing TimerTask", e);
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
        this.logMasterCheck();
        try (StatefulRedisSentinelConnection<String, String> connection = redisClient.connectSentinel()) {
            List<Map<String, String>> masters = connection.sync().masters();
            if (masters.isEmpty()) {
                logger.warn("Sentinel master node not found!");
                return Optional.empty();
            }
            return Optional.of(masters.get(0).get("ip"));
        }
    }

    private void logMasterCheck() {
        Consumer<String> loggerMethod = masterCheckLogCount == 0 ? logger::info : logger::debug;
        loggerMethod.accept("Checking Sentinel master IP");
        masterCheckLogCount = (masterCheckLogCount + 1) % 10;
    }
}
