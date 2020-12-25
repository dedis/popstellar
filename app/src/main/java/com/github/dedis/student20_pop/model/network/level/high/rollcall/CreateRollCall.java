package com.github.dedis.student20_pop.model.network.level.high.rollcall;

import androidx.annotation.Nullable;

import com.github.dedis.student20_pop.model.network.level.high.Action;
import com.github.dedis.student20_pop.model.network.level.high.Data;
import com.github.dedis.student20_pop.model.network.level.high.Objects;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Data sent to create a roll call
 */
public class CreateRollCall extends Data {

    private final String id;
    private final String name;
    private final long creation;
    private transient final long start;
    private transient final StartType startType;
    private final String location;
    @Nullable private transient final String description;

    public CreateRollCall(String id, String name, long creation, long start, StartType startType, String location, String description) {
        this.id = id;
        this.name = name;
        this.creation = creation;
        this.start = start;
        this.startType = startType;
        this.location = location;
        this.description = description;
    }

    @Override
    public String getObject() {
        return Objects.ROLL_CALL.getObject();
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

    public long getStartTime() {
        return start;
    }

    public StartType getStartType() {
        return startType;
    }

    public String getLocation() {
        return location;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateRollCall that = (CreateRollCall) o;
        return getCreation() == that.getCreation() &&
                start == that.start &&
                java.util.Objects.equals(getId(), that.getId()) &&
                java.util.Objects.equals(getName(), that.getName()) &&
                getStartType() == that.getStartType() &&
                java.util.Objects.equals(getLocation(), that.getLocation()) &&
                java.util.Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getId(), getName(), getCreation(), getStartTime(), getStartType(), getLocation(), getDescription());
    }

    /**
     * Enumeration of the different starting types of a roll call
     */
    public enum StartType {

        NOW("start"),
        SCHEDULED("scheduled");

        public static final List<StartType> ALL = Collections.unmodifiableList(Arrays.asList(StartType.values()));

        // Name of the time json member for that type
        private final String jsonType;

        StartType(String jsonType) {
            this.jsonType = jsonType;
        }

        public String getJsonMember() {
            return jsonType;
        }
    }
}
