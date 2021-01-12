package com.github.dedis.student20_pop.model.network.layer.base.answer;


import java.util.Objects;

/**
 * An abstract result from a request
 * <p>
 * Is linked to an earlier request with a unique id
 */
public abstract class Answer {

    private final int id;

    public Answer(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Answer answer = (Answer) o;
        return getId() == answer.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
