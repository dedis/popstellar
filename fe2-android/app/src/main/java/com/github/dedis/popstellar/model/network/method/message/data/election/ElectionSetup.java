package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.utility.MessageValidator;
import com.google.gson.annotations.SerializedName;

import java.util.*;
import java.util.stream.Collectors;

@Immutable
public class ElectionSetup extends Data {

  private final String electionId;
  private final String name;
  private final String laoId;

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
   * @param questions list of election questions
   */
  public ElectionSetup(
      @NonNull String name,
      long creation,
      long start,
      long end,
      @NonNull String laoId,
      @NonNull ElectionVersion electionVersion,
      @NonNull List<ElectionQuestion.Question> questions) {
    // The lao id is checked to be a known lao in the election setup handler
    MessageValidator.verify()
        .orderedTimes(creation, start, end)
        .validPastTimes(creation)
        .stringNotEmpty(name, "election name")
        .isBase64(laoId, "lao id")
        .noListDuplicates(questions);

    this.name = name;
    this.createdAt = creation;
    this.startTime = start;
    this.endTime = end;
    this.laoId = laoId;
    this.electionVersion = electionVersion;
    this.electionId = Election.generateElectionSetupId(laoId, createdAt, name);
    this.questions =
        questions.stream()
            .map(q -> new ElectionQuestion(this.electionId, q))
            .collect(Collectors.toList());
  }

  @Override
  public String getObject() {
    return Objects.ELECTION.getObject();
  }

  @Override
  public String getAction() {
    return Action.SETUP.getAction();
  }

  public String getElectionId() {
    return electionId;
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

  public String getLaoId() {
    return laoId;
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
        && java.util.Objects.equals(getElectionId(), that.getElectionId())
        && createdAt == that.getCreation()
        && java.util.Objects.equals(getName(), that.getName())
        && endTime == that.getEndTime()
        && java.util.Objects.equals(questions, that.getQuestions());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
        getElectionId(), getName(), getCreation(), getStartTime(), getEndTime(), getQuestions());
  }

  @NonNull
  @Override
  public String toString() {
    return "ElectionSetup={"
        + "version='"
        + electionVersion
        + '\''
        + ", id='"
        + electionId
        + '\''
        + ", lao='"
        + laoId
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
