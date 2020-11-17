package com.github.dedis.student20_pop.model.network.level.low.result;

import java.util.Objects;

/**
 * A failed request result
 */
public final class Failure extends Result {

    private final ResultError error;

    public Failure(int id, ResultError error) {
        super(id);
        this.error = error;
    }

    public ResultError getError() {
        return error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Failure failure = (Failure) o;
        return Objects.equals(getError(), failure.getError());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getError());
    }
}
