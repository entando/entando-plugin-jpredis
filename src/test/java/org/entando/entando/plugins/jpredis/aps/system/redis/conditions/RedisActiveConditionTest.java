package org.entando.entando.plugins.jpredis.aps.system.redis.conditions;

import org.springframework.context.annotation.Condition;

class RedisActiveConditionTest extends BaseRedisConditionTest {

    RedisActiveConditionTest() {
        super(RedisActive.class);
    }

    @Override
    Condition getCondition(boolean value) {
        return new RedisActiveCondition(value);
    }
}
