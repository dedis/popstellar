package com.github.dedis.student20_pop.model.network.level.high.meeting;

import com.github.dedis.student20_pop.model.network.level.high.Action;
import com.github.dedis.student20_pop.model.network.level.high.Message;
import com.github.dedis.student20_pop.model.network.level.high.Objects;

public class StateMeeting extends Message {

    private final String id; // Hash(lao_id + creation + name)
    private final String name;
    private final long creation;
    private final long last_modified;
    private final String location;
    private final long start;
    private final long end;

    public StateMeeting(String id, String name, long creation, long last_modified, String location, long start, long end) {
        this.id = id;
        this.name = name;
        this.creation = creation;
        this.last_modified = last_modified;
        this.location = location;
        this.start = start;
        this.end = end;
    }
    //private final Extra extra;

    @Override
    public String getObject() {
        return Objects.MEETING.getObject();
    }

    @Override
    public String getAction() {
        return Action.STATE.getAction();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCreation() {
        return creation;
    }

    public long getLast_modified() {
        return last_modified;
    }

    public String getLocation() {
        return location;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateMeeting that = (StateMeeting) o;
        return getCreation() == that.getCreation() &&
                getLast_modified() == that.getLast_modified() &&
                getStart() == that.getStart() &&
                getEnd() == that.getEnd() &&
                java.util.Objects.equals(getId(), that.getId()) &&
                java.util.Objects.equals(getName(), that.getName()) &&
                java.util.Objects.equals(getLocation(), that.getLocation());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getId(), getName(), getCreation(), getLast_modified(), getLocation(), getStart(), getEnd());
    }
}
