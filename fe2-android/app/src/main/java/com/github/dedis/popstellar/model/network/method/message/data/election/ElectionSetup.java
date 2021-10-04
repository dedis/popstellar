package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Election;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

  private final String version;
  private final List<ElectionQuestion> questions;

  /**
   * Constructor for a data setup Election Event
   *
   * @param name name of the Election
   * @param start of the Election
   * @param laoId id of the LAO
   */
  public ElectionSetup(
      String name,
      long start,
      long end,
      List<String> votingMethod,
      List<Boolean> writeIn,
      List<List<String>> ballotOptions,
      List<String> questionList,
      String laoId) {
    if (name == null
        || votingMethod == null
        || writeIn == null
        || ballotOptions == null
        || questionList == null
        || laoId == null) {
      throw new IllegalArgumentException();
    }
    if (end < 0 || start < 0 || end < start) {
      throw new IllegalArgumentException("Timestamp cannot be negative");
    }
    if (questionList.size() != votingMethod.size()
        || questionList.size() != writeIn.size()
        || questionList.size() != ballotOptions.size()) {
      throw new IllegalArgumentException("Lists are not of the same size");
    }
    this.name = name;
    this.createdAt = Instant.now().getEpochSecond();
    this.startTime = start;
    this.endTime = end;
    this.lao = laoId;
    this.version = "1.0.0";
    this.id = Election.generateElectionSetupId(laoId, createdAt, name);
    this.questions = new ArrayList<>();
    for (int i = 0; i < questionList.size(); i++) {
      this.questions.add(
          new ElectionQuestion(
              questionList.get(i),
              votingMethod.get(i),
              writeIn.get(i),
              ballotOptions.get(i),
              this.id));
    }
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
    return questions;
  }

  public String getLao() {
    return lao;
  }

  public String getVersion() {
    return version;
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

  @Override
  public String toString() {
    return "ElectionSetup{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", lao='"
        + lao
        + '\''
        + ", createdAt="
        + createdAt
        + ", startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", version='"
        + version
        + '\''
        + ", questions="
        + Arrays.toString(questions.toArray())
        + '}';
  }
}
