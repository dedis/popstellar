package com.github.dedis.student20_pop.model.network.layer.base;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Enumerate the different low level messages' method
 */
public enum Method {

    SUBSCRIBE("subscribe", Subscribe.class, true),
    UNSUBSCRIBE("unsubscribe", Unsubscribe.class, true),
    PUBLISH("publish", Publish.class, true),
    MESSAGE("broadcast", Broadcast.class, false),
    CATCHUP("catchup", Catchup.class, true);

    private static final List<Method> ALL = Collections.unmodifiableList(Arrays.asList(values()));

    private final String method;
    private final Class<? extends Message> dataClass;
    private final boolean expectResult;

    Method(String method, Class<? extends Message> dataClass, boolean expectResult) {
        this.method = method;
        this.dataClass = dataClass;
        this.expectResult = expectResult;
    }

    public static Method find(String method) {
        for (Method a : ALL)
            if (a.method.equals(method))
                return a;
        return null;
    }

    public String getMethod() {
        return method;
    }

    public Class<? extends Message> getDataClass() {
        return dataClass;
    }

    public boolean expectResult() {
        return expectResult;
    }
}
