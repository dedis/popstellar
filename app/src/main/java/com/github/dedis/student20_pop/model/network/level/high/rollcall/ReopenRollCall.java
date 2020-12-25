package com.github.dedis.student20_pop.model.network.level.high.rollcall;

import com.github.dedis.student20_pop.model.network.level.high.Action;
import com.github.dedis.student20_pop.model.network.level.high.Data;
import com.github.dedis.student20_pop.model.network.level.high.Objects;

/**
 * Data sent to reopen a roll call
 */
public class ReopenRollCall extends Data {

    private final String id;
    private final long start;

    public ReopenRollCall(String id, long start) {
        this.id = id;
        this.start = start;
    }

    @Override
    public String getObject() {
        return Objects.ROLL_CALL.getObject();
    }

    @Override
    public String getAction() {
        return Action.REOPEN.getAction();
    }

    public String getId() {
        return id;
    }

    public long getStart() {
        return start;
    }
}
