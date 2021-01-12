package com.github.dedis.student20_pop.model.network.layer.base;

import java.util.Objects;

/**
 * A message that expect an answer later.
 * Therefore, it has a unique id that will be linked with the received answer
 */
public abstract class Query extends Message {

    private final transient int id;

    protected Query(String channel, int id) {
        super(channel);
        this.id = id;
    }

    public int getRequestID() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Query query = (Query) o;
        return id == query.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
