package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.utility.MessageValidator.verify
import com.google.gson.annotations.SerializedName
import java.util.Objects

@Immutable
class EncryptedVote : Vote {
  // Id of the vote
  // Hash(“Vote”||election_id||question_id||(encrypted_vote_index(es)|encrypted_write_in))
  override val id: String

  // Id of the question
  @SerializedName(value = "question") override val questionId: String

  // Encrypted vote index
  val vote: String?

  /**
   * @param questionId id of the question
   * @param encryptedVote encrypted unique indices of the chosen vote
   * @param writeInEnabled indicates if write in is enabled
   * @param encryptedWriteIn whether the election allows write-in
   * @param electionId the election id
   */
  constructor(
      questionId: String,
      encryptedVote: String?,
      writeInEnabled: Boolean,
      encryptedWriteIn: String?,
      electionId: String,
  ) {
    // Assumes if write-in is disabled, encryptedVote must be non-null and non-empty. If write-in is
    // enabled,
    // encryptedVote is ignored, and encryptedWriteIn must be non-null and non-empty.
    // TODO: Need to verify the exact intended behavior when write-in is supported.
    verify()
        .isNotEmptyBase64(questionId, "question ID")
        .isNotEmptyBase64(electionId, "election ID")
        .apply {
          if (writeInEnabled) {
            isNotEmptyBase64(encryptedWriteIn, "write-in")
          } else {
            isNotEmptyBase64(encryptedVote, "encrypted vote")
          }
        }

    this.questionId = questionId
    this.id =
        Election.generateEncryptedElectionVoteId(
            electionId,
            questionId,
            encryptedVote,
            encryptedWriteIn,
            writeInEnabled,
        )
    this.vote =
        if (writeInEnabled) {
          null
        } else {
          encryptedVote
        }
  }

  constructor(id: String, question: String, vote: String) {
    verify()
        .stringNotEmpty(id, "vote ID")
        .stringNotEmpty(question, "question")
        .isNotEmptyBase64(vote, "vote")

    this.id = id
    this.questionId = question
    this.vote = vote
  }

  override val isEncrypted: Boolean
    get() = true

  override fun hashCode(): Int {
    return Objects.hash(id, vote, questionId)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as EncryptedVote
    return questionId == that.questionId && id == that.id && vote == that.vote
  }

  override fun toString(): String {
    return "{id='$id', questionId='$questionId', vote=$vote}"
  }
}
