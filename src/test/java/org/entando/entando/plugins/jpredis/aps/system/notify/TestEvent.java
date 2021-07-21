package org.entando.entando.plugins.jpredis.aps.system.notify;

import com.agiletec.aps.system.common.IManager;
import com.agiletec.aps.system.common.notify.ApsEvent;
import java.util.Map;
import org.entando.entando.ent.exception.EntException;

public class TestEvent extends ApsEvent {

    public TestEvent() {
        super();
    }

    public TestEvent(String channel, Map<String, String> properties) throws EntException {
        super(channel, properties);
    }

    @Override
    public void notify(IManager iManager) {

    }

    @Override
    public Class getObserverInterface() {
        return null;
    }
}
