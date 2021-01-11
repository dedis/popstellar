package com.github.dedis.student20_pop.model.network.level.high.rollcall;

import com.github.dedis.student20_pop.model.network.level.high.Action;
import com.github.dedis.student20_pop.model.network.level.high.Data;
import com.github.dedis.student20_pop.model.network.level.high.Objects;

/**
 * Data sent to open a roll call
 */
public class OpenRollCall extends Data {

    private final String id;
    private final long start;

    public OpenRollCall(String id, long start) {
        this.id = id;
        this.start = start;
    }

    @Override
    public String getObject() {
        return Objects.ROLL_CALL.getObject();
    }

    @Override
    public String getAction() {
        return Action.OPEN.getAction();
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
        OpenRollCall that = (OpenRollCall) o;
        return getStart() == that.getStart() &&
                java.util.Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getId(), getStart());
    }

    @Override
    public String toString() {
        return "OpenRollCall{" +
                "id='" + id + '\'' +
                ", start=" + start +
                '}';
    }
}
