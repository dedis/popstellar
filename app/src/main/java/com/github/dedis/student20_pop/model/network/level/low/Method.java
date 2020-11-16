package com.github.dedis.student20_pop.model.network.level.low;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Method {

    SUBSCRIBE("subscribe", Subscribe.class, true),
    UNSUBSCRIBE("unsubscribe", Unsubscribe.class, true),
    PUBLISH("publish", Publish.class, true),
    MESSAGE("message", LowLevelMessage.class, false),
    CATCHUP("catchup", Catchup.class, true);

    private static final List<Method> ALL = Collections.unmodifiableList(Arrays.asList(values()));

    private final String method;
    private final Class<? extends ChanneledMessage> dataClass;
    private final boolean expectResult;

    Method(String method, Class<? extends ChanneledMessage> dataClass, boolean expectResult) {
        this.method = method;
        this.dataClass = dataClass;
        this.expectResult = expectResult;
    }

    public String getMethod() {
        return method;
    }

    public Class<? extends ChanneledMessage> getDataClass() {
        return dataClass;
    }

    public static Method find(String method) {
        for(Method a : ALL)
            if(a.method.equals(method))
                return a;
        return null;
    }

    public boolean expectResult() {
        return expectResult;
    }
}
