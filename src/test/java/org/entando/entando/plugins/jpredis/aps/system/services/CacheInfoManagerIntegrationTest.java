/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpredis.aps.system.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.SystemConstants;
import org.entando.entando.aps.system.services.cache.CacheInfoManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Classe test del servizio gestore cache.
 *
 * @author E.Santoboni
 */
class CacheInfoManagerIntegrationTest extends BaseTestCase {

	private static final String DEFAULT_CACHE = CacheInfoManager.DEFAULT_CACHE_NAME;

	private CacheInfoManager cacheInfoManager = null;

	@BeforeEach
    public void init() throws Exception {
        try {
            cacheInfoManager = (CacheInfoManager) BaseTestCase.getApplicationContext().getBean(SystemConstants.CACHE_INFO_MANAGER);
        } catch (Throwable t) {
            throw new Exception(t);
        }
    }
    
    @Test
	void testPutGetFromCache_1() {
		String value = "Stringa prova";
		String key = "Chiave_prova";
		this.cacheInfoManager.putInCache(DEFAULT_CACHE, key, value);
		Object extracted = this.cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertEquals(value, extracted);
		this.cacheInfoManager.flushEntry(DEFAULT_CACHE, key);
		extracted = this.cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertNull(extracted);
	}
    
    @Test
	void testPutGetFromCache_2() throws Throwable {
		String key = "Chiave_prova";
		Object extracted = this.cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertNull(extracted);
		extracted = this.cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertNull(extracted);

		String value = "Stringa prova";
		this.cacheInfoManager.putInCache(DEFAULT_CACHE, key, value);

		extracted = this.cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertNotNull(extracted);
		assertEquals(value, extracted);
		this.cacheInfoManager.flushEntry(DEFAULT_CACHE, key);
		synchronized (this) {
			this.wait(1000);
		}
		extracted = this.cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertNull(extracted);
	}

	@Test
	void testPutGetFromCacheOnRefreshPeriod() throws Throwable {
		String value = "Stringa prova";
		String key = "Chiave prova";
		this.cacheInfoManager.putInCache(DEFAULT_CACHE, key, value);
		this.cacheInfoManager.setExpirationTime(DEFAULT_CACHE, key, 2l);
		Object extracted = this.cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertEquals(value, extracted);
		synchronized (this) {
			this.wait(3000);
		}
		extracted = this.cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertNull(extracted);
	}

	@Test
	void testPutGetFromCacheGroup() {
		String value = "Stringa prova";
		String key = "Chiave prova";
		String group1 = "group1";
		String[] groups = {group1};
		cacheInfoManager.putInCache(DEFAULT_CACHE, key, value, groups);
		Object extracted = cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertEquals(value, extracted);
		cacheInfoManager.flushGroup(DEFAULT_CACHE, group1);
		extracted = cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertNull(extracted);
	}
    
}
