package com.github.dedis.student20_pop.model.network.level.low.result;

import java.util.Objects;

public final class ResultError {

    private final int code;
    private final String description;

    public ResultError(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResultError that = (ResultError) o;
        return getCode() == that.getCode() &&
                Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCode(), getDescription());
    }
}
