package com.github.dedis.student20_pop.model.network.layer.data.rollcall;

import com.github.dedis.student20_pop.model.network.layer.data.Action;
import com.github.dedis.student20_pop.model.network.layer.data.Data;
import com.github.dedis.student20_pop.model.network.layer.data.Objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Data sent to close a roll call
 */
public class CloseRollCall extends Data {

    private final String id;
    private final long start;
    private final long end;
    private final List<String> attendees;

    public CloseRollCall(String id, long start, long end, List<String> attendees) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.attendees = new ArrayList<>(attendees);
    }

    @Override
    public String getObject() {
        return Objects.ROLL_CALL.getObject();
    }

    @Override
    public String getAction() {
        return Action.CLOSE.getAction();
    }

    public String getId() {
        return id;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public List<String> getAttendees() {
        return attendees;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CloseRollCall that = (CloseRollCall) o;
        return getStart() == that.getStart() &&
                getEnd() == that.getEnd() &&
                java.util.Objects.equals(getId(), that.getId()) &&
                java.util.Objects.equals(getAttendees(), that.getAttendees());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getId(), getStart(), getEnd(), getAttendees());
    }

    @Override
    public String toString() {
        return "CloseRollCall{" +
                "id='" + id + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", attendees=" + attendees +
                '}';
    }
}
