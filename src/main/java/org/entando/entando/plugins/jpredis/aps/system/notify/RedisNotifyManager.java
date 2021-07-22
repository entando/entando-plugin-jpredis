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
package org.entando.entando.plugins.jpredis.aps.system.notify;

import com.agiletec.aps.system.common.notify.ApsEvent;
import com.agiletec.aps.system.common.notify.NotifyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class RedisNotifyManager extends NotifyManager {
    
    private static final Logger logger = EntLogFactory.getSanitizedLogger(RedisNotifyManager.class);
    
    private transient StatefulRedisPubSubConnection<String, String> pubConn;
    private transient StatefulRedisPubSubConnection<String, String> subConn;
    
    @Autowired(required = false)
    private transient RedisClient redisClient;
    
    public void destroy() {
        if (null != pubConn) {
            this.pubConn.close();
        }
        if (null != subConn) {
            this.subConn.close();
        }
    }

    @Override
    protected void notify(ApsEvent event) {
        if (null != event.getObserverInterface()) {
            super.notify(event);
        }
        if (null == this.redisClient) {
            return;
        }
        if (null != event.getChannel() && null != event.getMessage()) {
            String channel = event.getChannel();
            String message = event.getMessage();
            StatefulRedisPubSubConnection<String, String> pubConnection = this.getPubConnection();
            if (null != pubConnection) {
                pubConnection.async().publish(channel, message);
            } else {
                logger.error("notify - null StatefulRedisPubSubConnection from RedisClient {} ", this.redisClient);
            }
        }
    }

    public void addListener(String channel, RedisPubSubListener<String, String> listener) {
        if (null == this.redisClient) {
            logger.warn("Redis not active - listener not added");
            return;
        }
        StatefulRedisPubSubConnection<String, String> connection = this.getSubConnection();
        if (null != connection) {
            connection.addListener(listener);
            connection.async().subscribe(channel);
            logger.info("Registered listener {} - channel {}", listener.getClass().getName(), channel);
        } else {
            logger.error("subscribe - null StatefulRedisPubSubConnection from RedisClient {} ", this.redisClient);
        }
    }
    
    private StatefulRedisPubSubConnection<String, String> getSubConnection() {
        if (null == this.subConn) {
            subConn = this.redisClient.connectPubSub();
        }
        return this.subConn;
    }
    
    private StatefulRedisPubSubConnection<String, String> getPubConnection() {
        if (null == this.pubConn) {
            pubConn = this.redisClient.connectPubSub();
        }
        return this.pubConn;
    }

}
