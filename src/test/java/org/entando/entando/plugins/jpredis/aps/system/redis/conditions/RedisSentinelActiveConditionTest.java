package org.entando.entando.plugins.jpredis.aps.system.redis.conditions;

import org.springframework.context.annotation.Condition;

class RedisSentinelActiveConditionTest extends BaseRedisConditionTest {

    RedisSentinelActiveConditionTest() {
        super(RedisSentinel.class);
    }

    @Override
    Condition getCondition(boolean value) {
        return new RedisSentinelActiveCondition(value);
    }
}
