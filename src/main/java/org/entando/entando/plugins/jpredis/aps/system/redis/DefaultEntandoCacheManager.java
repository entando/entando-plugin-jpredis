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

import java.util.Collection;
import java.util.List;
import org.entando.entando.aps.system.services.cache.EntandoCacheManager;
import org.entando.entando.aps.system.services.cache.ExternalCachesContainer;
import org.springframework.cache.Cache;

/**
 * @author E.Santoboni
 */
public class DefaultEntandoCacheManager extends EntandoCacheManager {

    @Override
    protected void setExternalCachesContainers(List<ExternalCachesContainer> externalCachesContainers) {
        super.setExternalCachesContainers(externalCachesContainers);
    }

    @Override
    public void setCaches(Collection<Cache> caches) {
        super.setCaches(caches);
    }
    
}
