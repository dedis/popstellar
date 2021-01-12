package com.github.dedis.student20_pop.model.network.layer.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Enumerates all possible messages actions
 */
public enum Action {

    CREATE("create"),
    UPDATE("update_properties"),
    STATE("state"),
    WITNESS("witness"),
    OPEN("open"),
    REOPEN("reopen"),
    CLOSE("close");

    private static final List<Action> ALL = Collections.unmodifiableList(Arrays.asList(values()));
    private final String action;

    Action(String action) {
        this.action = action;
    }

    public static Action find(String searched) {
        for (Action action : ALL)
            if (action.getAction().equals(searched))
                return action;

        return null;
    }

    public String getAction() {
        return action;
    }
}
