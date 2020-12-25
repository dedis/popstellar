package com.github.dedis.student20_pop.model.network.level.high;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Enumerates all possible messages objects
 */
public enum Objects {

    LAO("lao"),
    MEETING("meeting"),
    MESSAGE("message"),
    ROLL_CALL("roll-cal_");

    private static final List<Objects> ALL = Collections.unmodifiableList(Arrays.asList(values()));
    private final String object;

    Objects(String object) {
        this.object = object;
    }

    public static Objects find(String searched) {
        for (Objects object : ALL)
            if (object.getObject().equals(searched))
                return object;

        return null;
    }

    public String getObject() {
        return object;
    }
}
