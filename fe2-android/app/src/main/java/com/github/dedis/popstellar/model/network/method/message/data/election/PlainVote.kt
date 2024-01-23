package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.Election
import com.google.gson.annotations.SerializedName
import java.util.Objects

@Immutable
class PlainVote : Vote {
  // Id of the vote
  // Hash('Vote'||election_id||question_id||(vote_index(es)|write_in))
  override val id: String

  // Id of the object ElectionVote
  @SerializedName(value = "question") override val questionId: String

  // index of the chosen vote
  val vote: Int?

  /**
   * Constructor for a data Vote, for cast vote . It represents a Vote for one Question.
   *
   * @param questionId the Id of the question
   * @param vote the list of indexes for the ballot options chose by the voter
   * @param writeInEnabled parameter to know if write is enabled or not
   * @param writeIn string corresponding to the write_in
   * @param electionId Id of the election
   */
  constructor(
    questionId: String,
    vote: Int?,
    writeInEnabled: Boolean,
    writeIn: String?,
    electionId: String?
  ) {
    this.questionId = questionId
    this.vote = if (writeInEnabled) null else vote
    this.id = Election.generateElectionVoteId(electionId, questionId, vote, writeIn, writeInEnabled)
  }

  constructor(id: String, question: String, vote: Int) {
    this.id = id
    this.questionId = question
    this.vote = vote
  }

  override val isEncrypted: Boolean
    get() = false

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as PlainVote
    return questionId == that.questionId && id == that.id && vote == that.vote
  }

  override fun hashCode(): Int {
    return Objects.hash(id, vote, questionId)
  }

  override fun toString(): String {
    return "ElectionVote{id='$id', questionId='$questionId', vote=$vote}"
  }
}
