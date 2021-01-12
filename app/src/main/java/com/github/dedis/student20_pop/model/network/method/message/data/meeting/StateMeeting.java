package com.github.dedis.student20_pop.model.network.method.message.data.meeting;

import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;

/**
 * Data received to track the state of a meeting
 */
public class StateMeeting extends Data {

    private final String id;
    private final String name;
    private final long creation;
    private final long lastModified;
    private final String location;
    private final long start;
    private final long end;
    //private final Extra extra;

    /**
     * Constructor for a data State Meeting Event
     *
     * @param id of the state Meeting message, Hash("M"||laoId||creation||name)
     * @param name name of the Meeting
     * @param creation time of creation
     * @param lastModified time of the last modification
     * @param location location of the Meeting
     * @param start of the Meeting
     * @param end of the Meeting
     */
    public StateMeeting(String id, String name, long creation, long lastModified, String location, long start, long end) {
        this.id = id;
        this.name = name;
        this.creation = creation;
        this.lastModified = lastModified;
        this.location = location;
        this.start = start;
        this.end = end;
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

    public long getLastModified() {
        return lastModified;
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
    public String getObject() {
        return Objects.MEETING.getObject();
    }

    @Override
    public String getAction() {
        return Action.STATE.getAction();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateMeeting that = (StateMeeting) o;
        return getCreation() == that.getCreation() &&
                getLastModified() == that.getLastModified() &&
                getStart() == that.getStart() &&
                getEnd() == that.getEnd() &&
                java.util.Objects.equals(getId(), that.getId()) &&
                java.util.Objects.equals(getName(), that.getName()) &&
                java.util.Objects.equals(getLocation(), that.getLocation());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getId(), getName(), getCreation(), getLastModified(), getLocation(), getStart(), getEnd());
    }

    @Override
    public String toString() {
        return "StateMeeting{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", creation=" + creation +
                ", last_modified=" + lastModified +
                ", location='" + location + '\'' +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
