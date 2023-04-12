package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.Election;
import com.google.gson.annotations.SerializedName;

import java.util.*;
import java.util.stream.Collectors;

@Immutable
public class ElectionSetup extends Data {

  private final String id;
  private final String name;
  private final String lao;

  @SerializedName(value = "created_at")
  private final long createdAt;

  @SerializedName(value = "start_time")
  private final long startTime;

  @SerializedName(value = "end_time")
  private final long endTime;

  @SerializedName("version")
  private final ElectionVersion electionVersion;

  private final List<ElectionQuestion> questions;

  /**
   * @param name name of the election
   * @param creation creation timestamp
   * @param start start timestamp
   * @param end end timestamp
   * @param laoId id of the LAO
   * @param electionVersion version of the election
   * @param questions information
   */
  public ElectionSetup(
      @NonNull String name,
      long creation,
      long start,
      long end,
      @NonNull String laoId,
      @NonNull ElectionVersion electionVersion,
      @NonNull List<ElectionQuestion.Question> questions) {
    // we don't need to check if end < 0 or start < 0 as it is already covered by other statements
    if (creation < 0 || start < creation || end < start) {
      throw new IllegalArgumentException("Timestamp cannot be negative");
    }

    this.name = name;
    this.createdAt = creation;
    this.startTime = start;
    this.endTime = end;
    this.lao = laoId;
    this.electionVersion = electionVersion;
    this.id = Election.generateElectionSetupId(laoId, createdAt, name);
    this.questions =
        questions.stream().map(q -> new ElectionQuestion(this.id, q)).collect(Collectors.toList());
  }

  @Override
  public String getObject() {
    return Objects.ELECTION.getObject();
  }

  @Override
  public String getAction() {
    return Action.SETUP.getAction();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public long getCreation() {
    return createdAt;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public List<ElectionQuestion> getQuestions() {
    return new ArrayList<>(questions);
  }

  public String getLao() {
    return lao;
  }

  public ElectionVersion getElectionVersion() {
    return electionVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElectionSetup that = (ElectionSetup) o;
    return getCreation() == that.getCreation()
        && getElectionVersion() == that.getElectionVersion()
        && startTime == that.getStartTime()
        && java.util.Objects.equals(getId(), that.getId())
        && createdAt == that.getCreation()
        && java.util.Objects.equals(getName(), that.getName())
        && endTime == that.getEndTime()
        && java.util.Objects.equals(questions, that.getQuestions());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
        getId(), getName(), getCreation(), getStartTime(), getEndTime(), getQuestions());
  }

  @NonNull
  @Override
  public String toString() {
    return "ElectionSetup={"
        + "version='"
        + electionVersion
        + '\''
        + ", id='"
        + id
        + '\''
        + ", lao='"
        + lao
        + '\''
        + ", name='"
        + name
        + '\''
        + ", createdAt="
        + createdAt
        + ", startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", questions="
        + Arrays.toString(questions.toArray())
        + '}';
  }
}