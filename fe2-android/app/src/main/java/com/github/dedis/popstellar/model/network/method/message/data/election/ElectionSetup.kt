package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion.Question
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.utility.MessageValidator.verify
import com.google.gson.annotations.SerializedName
import java.util.stream.Collectors

@Immutable
class ElectionSetup(
  name: String,
  creation: Long,
  start: Long,
  end: Long,
  laoId: String,
  electionVersion: ElectionVersion,
  questions: List<Question>
) : Data() {
  val id: String
  val name: String
  val lao: String

  @SerializedName(value = "created_at") val creation: Long

  @SerializedName(value = "start_time") val startTime: Long

  @SerializedName(value = "end_time") val endTime: Long

  @SerializedName("version") val electionVersion: ElectionVersion
  val questions: List<ElectionQuestion>
    get() = ArrayList(field)

  /**
   * @param name name of the election
   * @param creation creation timestamp
   * @param start start timestamp
   * @param end end timestamp
   * @param laoId id of the LAO
   * @param electionVersion version of the election
   * @param questions list of election questions
   */
  init {
    // The lao id is checked to be a known lao in the election setup handler
    verify()
      .orderedTimes(creation, start, end)
      .validPastTimes(creation)
      .stringNotEmpty(name, "election name")
      .isNotEmptyBase64(laoId, "lao id")
      .noListDuplicates(questions)

    this.name = name
    this.creation = creation
    this.startTime = start
    this.endTime = end
    this.lao = laoId
    this.electionVersion = electionVersion
    this.id = Election.generateElectionSetupId(laoId, this.creation, name)
    this.questions =
      questions.stream().map { q: Question -> ElectionQuestion(id, q) }.collect(Collectors.toList())
  }

  override val `object`: String
    get() = Objects.ELECTION.`object`

  override val action: String
    get() = Action.SETUP.action

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ElectionSetup
    return creation == that.creation &&
      electionVersion == that.electionVersion &&
      startTime == that.startTime &&
      id == that.id &&
      name == that.name &&
      endTime == that.endTime &&
      questions == that.questions
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(id, name, creation, startTime, endTime, questions)
  }

  override fun toString(): String {
    return "ElectionSetup={version='$electionVersion', id='$id', lao='$lao', name='$name', createdAt=$creation, startTime=$startTime, endTime=$endTime, questions=${
      questions.toTypedArray().contentToString()
    }}"
  }
}
