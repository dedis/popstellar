package com.github.dedis.student20_pop.model.network.level.low.result;

import com.google.gson.JsonElement;

import java.util.Objects;

public final class Success extends Result {

    private final JsonElement result;

    public Success(int id, JsonElement result) {
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
        if(!super.equals(o)) return false;
        Success success = (Success) o;
        return Objects.equals(getResult(), success.getResult());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getResult());
    }
}
