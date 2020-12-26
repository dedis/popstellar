package com.github.dedis.student20_pop.model.network.level.low.answer;

import java.util.Objects;

/**
 * Error of a failed request
 */
public final class ErrorCode {

    private final int code;
    private final String description;

    public ErrorCode(int code, String description) {
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
        ErrorCode that = (ErrorCode) o;
        return getCode() == that.getCode() &&
                Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCode(), getDescription());
    }
}
