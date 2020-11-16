package com.github.dedis.student20_pop.model.network.level.low.result;


import java.util.Objects;

public abstract class Result {

    private final int id;

    public Result(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Result result = (Result) o;
        return getId() == result.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
