package com.github.dedis.student20_pop.model.network.level.low.answer;

import com.google.gson.JsonElement;

import java.util.Objects;

/**
 * A succeed query's answer
 */
public final class Result extends Answer {

    private final JsonElement result;

    public Result(int id, JsonElement result) {
        super(id);
        this.result = result;
    }

    public JsonElement getResult() {
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Result result = (Result) o;
        return Objects.equals(getResult(), result.getResult());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getResult());
    }
}
