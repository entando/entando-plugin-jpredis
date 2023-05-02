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

import org.entando.entando.TestEntandoJndiUtils;
import org.entando.entando.aps.system.services.cache.CacheInfoManager;
import org.entando.entando.plugins.jpredis.utils.RedisTestExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Classe test del servizio gestore cache.
 *
 * @author E.Santoboni
 */
@ExtendWith(RedisTestExtension.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
		"classpath*:spring/testpropertyPlaceholder.xml",
		"classpath*:spring/baseSystemConfig.xml",
		"classpath*:spring/aps/**/**.xml",
		"classpath*:spring/plugins/**/aps/**/**.xml",
		"classpath*:spring/web/**.xml"
})
@WebAppConfiguration(value = "")
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class CacheInfoManagerIntegrationTest {

	private static final String DEFAULT_CACHE = CacheInfoManager.DEFAULT_CACHE_NAME;

	@BeforeAll
	static void setUp() {
		TestEntandoJndiUtils.setupJndi();
	}

	@Autowired
	private CacheInfoManager cacheInfoManager;

	@Test
	void testPutGetFromCache_1() throws Throwable {
		String value = "Stringa prova";
		String key = "Chiave_prova_1";
		this.cacheInfoManager.putInCache(DEFAULT_CACHE, key, value);
		Object extracted = this.cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertEquals(value, extracted);
		this.cacheInfoManager.flushEntry(DEFAULT_CACHE, key);
		synchronized (this) {
			this.wait(1000);
		}
		extracted = this.cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertNull(extracted);
	}

	@Test
	void testPutGetFromCache_2() throws Throwable {
		String key = "Chiave_prova_2";
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
		String key = "Chiave_prova_3";
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
		String key = "Chiave_prova_4";
		String group = "group4";
		String[] groups = {group};
		cacheInfoManager.putInCache(DEFAULT_CACHE, key, value, groups);
		Object extracted = cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertEquals(value, extracted);
		cacheInfoManager.flushGroup(DEFAULT_CACHE, group);
		extracted = cacheInfoManager.getFromCache(DEFAULT_CACHE, key);
		assertNull(extracted);
	}

}
