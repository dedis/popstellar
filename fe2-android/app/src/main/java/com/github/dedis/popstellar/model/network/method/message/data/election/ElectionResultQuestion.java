package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;

import java.util.*;

@Immutable
public class ElectionResultQuestion {

  private final String id;
  private final Set<QuestionResult> result;

  public ElectionResultQuestion(@NonNull String id, @NonNull Set<QuestionResult> result) {
    if (result.isEmpty()) {
      throw new IllegalArgumentException();
    }
    this.id = id;
    this.result = new HashSet<>(result);
  }

  public @NonNull String getId() {
    return id;
  }

  public @NonNull Set<QuestionResult> getResult() {
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElectionResultQuestion that = (ElectionResultQuestion) o;

    return Objects.equals(id, that.id) && Objects.equals(result, that.result);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, result);
  }

  @NonNull
  @Override
  public String toString() {
    return "ElectionResultQuestion{"
        + "id='"
        + id
        + '\''
        + ", result="
        + Arrays.toString(result.toArray())
        + '}';
  }
}
