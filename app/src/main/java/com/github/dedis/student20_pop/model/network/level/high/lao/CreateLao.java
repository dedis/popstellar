package com.github.dedis.student20_pop.model.network.level.high.lao;

import com.github.dedis.student20_pop.model.network.level.high.Action;
import com.github.dedis.student20_pop.model.network.level.high.Message;
import com.github.dedis.student20_pop.model.network.level.high.Objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Message sent when creating a new lao
 */
public class CreateLao extends Message {

    private final String id; //Hash (organizer + creation + name)
    private final String name;
    private final long creation;
    private final long last_modified;
    private final String organizer;
    private final List<String> witnesses;

    public CreateLao(String id, String name, long creation, long last_modified, String organizer, List<String> witnesses) {
        this.id = id;
        this.name = name;
        this.creation = creation;
        this.last_modified = last_modified;
        this.organizer = organizer;
        this.witnesses = witnesses;
    }

    @Override
    public String getObject() {
        return Objects.LAO.getObject();
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

    public String getOrganizer() {
        return organizer;
    }

    public List<String> getWitnesses() {
        return new ArrayList<>(witnesses);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateLao createLao = (CreateLao) o;
        return getCreation() == createLao.getCreation() &&
                getLast_modified() == createLao.getLast_modified() &&
                java.util.Objects.equals(getId(), createLao.getId()) &&
                java.util.Objects.equals(getName(), createLao.getName()) &&
                java.util.Objects.equals(getOrganizer(), createLao.getOrganizer()) &&
                java.util.Objects.equals(getWitnesses(), createLao.getWitnesses());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getId(), getName(), getCreation(), getLast_modified(), getOrganizer(), getWitnesses());
    }
}
