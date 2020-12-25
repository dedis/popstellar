package com.github.dedis.student20_pop.model.network.level.high.meeting;

import com.github.dedis.student20_pop.model.network.level.high.Action;
import com.github.dedis.student20_pop.model.network.level.high.Data;
import com.github.dedis.student20_pop.model.network.level.high.Objects;

/**
 * Data sent to create a new meeting
 */
public class CreateMeeting extends Data {

    private final String id; // Hash(lao_id + creation + name)
    private final String name;
    private final long creation;
    private final long last_modified;
    private final String location;
    private final long start;
    private final long end;
    //private final Extra extra;

    public CreateMeeting(String id, String name, long creation, long last_modified, String location, long start, long end) {
        this.id = id;
        this.name = name;
        this.creation = creation;
        this.last_modified = last_modified;
        this.location = location;
        this.start = start;
        this.end = end;
    }
    // private Extra extra;

    @Override
    public String getObject() {
        return Objects.MEETING.getObject();
    }

    @Override
    public String getAction() {
        return Action.CREATE.getAction();
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
        CreateMeeting that = (CreateMeeting) o;
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

    @Override
    public String toString() {
        return "CreateMeeting{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", creation=" + creation +
                ", last_modified=" + last_modified +
                ", location='" + location + '\'' +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
