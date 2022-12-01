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
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author E.Santoboni
 */
public class SentinelScheduler extends TimerTask {
    
    private static final Logger logger = LoggerFactory.getLogger(SentinelScheduler.class);
	
	private Timer timer;
	private RedisClient lettuceClient;
	private CacheConfig cacheConfig;
    private String currentMasterIp;
	
	public SentinelScheduler(RedisClient lettuceClient, int delaySeconds, CacheConfig cacheConfig) {
        this.cacheConfig = cacheConfig;
		this.lettuceClient = lettuceClient;
		this.timer = new Timer();
        Calendar startTime = Calendar.getInstance();
        startTime.add(Calendar.SECOND, delaySeconds);
		this.timer.schedule(this, startTime.getTime(), delaySeconds*1000l);
        List<Map<String, String>> masters = lettuceClient.connectSentinel().sync().masters();
        this.currentMasterIp = (!masters.isEmpty()) ? masters.get(0).get("ip") : null;
        logger.info("CURRENT master node '{}'", this.currentMasterIp);
	}
	
	@Override
	public boolean cancel() {
		this.timer.cancel();
		return super.cancel();
	}
	
    @Override
    public void run() {
        try {
            List<Map<String, String>> masters = lettuceClient.connectSentinel().sync().masters();
            String ip = (!masters.isEmpty()) ? masters.get(0).get("ip") : null;
            if (null != this.currentMasterIp && !this.currentMasterIp.equals(ip)) {
                logger.info("Refresh of front-end-cache -> from master node '{}' to '{}'", this.currentMasterIp, ip);
                cacheConfig.rebuildCacheFrontend(this.lettuceClient);
                this.currentMasterIp = ip;
            }
        } catch (Exception e) {
            throw new EntRuntimeException("Error on executing TimerTask", e);
        }
    }
	
}
