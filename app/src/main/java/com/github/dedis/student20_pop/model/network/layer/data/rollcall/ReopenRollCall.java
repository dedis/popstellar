package com.github.dedis.student20_pop.model.network.layer.data.rollcall;

import com.github.dedis.student20_pop.model.network.layer.data.Action;
import com.github.dedis.student20_pop.model.network.layer.data.Data;
import com.github.dedis.student20_pop.model.network.layer.data.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReopenRollCall that = (ReopenRollCall) o;
        return getStart() == that.getStart() &&
                java.util.Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getId(), getStart());
    }

    @Override
    public String toString() {
        return "ReopenRollCall{" +
                "id='" + id + '\'' +
                ", start=" + start +
                '}';
    }
}
