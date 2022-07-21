package com.github.dedis.popstellar.model.network.method.message.data.election;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Election;
import com.google.gson.annotations.SerializedName;

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

  @SerializedName("version")
  private final ElectionVersion electionVersion;

  private final List<ElectionQuestion> questions;

  /**
   * @param writeIn write in for questions
   * @param name name of the election
   * @param creation creation timestamp
   * @param start start timestamp
   * @param end end timestamp
   * @param votingMethod voting methods
   * @param laoId id of the LAO
   * @param ballotOptions ballot options
   * @param questionList list of questions
   * @param electionVersion version of the election
   */
  public ElectionSetup(
      List<Boolean> writeIn,
      String name,
      long creation,
      long start,
      long end,
      List<String> votingMethod,
      String laoId,
      List<List<String>> ballotOptions,
      List<String> questionList,
      ElectionVersion electionVersion) {
    if (name == null
        || votingMethod == null
        || writeIn == null
        || ballotOptions == null
        || questionList == null
        || laoId == null) {
      throw new IllegalArgumentException();
    }
    // we don't need to check if end < 0 or start < 0 as it is already covered by other statements
    if (creation < 0 || start < creation || end < start) {
      throw new IllegalArgumentException("Timestamp cannot be negative");
    }
    if (questionList.size() != votingMethod.size()
        || questionList.size() != writeIn.size()
        || questionList.size() != ballotOptions.size()) {
      throw new IllegalArgumentException("Lists are not of the same size");
    }
    this.name = name;
    this.createdAt = creation;
    this.startTime = start;
    this.endTime = end;
    this.lao = laoId;
    this.electionVersion = electionVersion;
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
