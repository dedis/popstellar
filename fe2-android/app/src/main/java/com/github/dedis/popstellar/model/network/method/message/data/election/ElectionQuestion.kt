package com.github.dedis.popstellar.model.network.method.message.data.election

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.Election
import com.google.gson.annotations.SerializedName
import java.util.Collections
import java.util.Objects

@Immutable
/** Constructor for a data Question, for the election setup */
class ElectionQuestion(electionId: String, question: Question) {
  val question: String = question.title
  val id: String = Election.generateElectionQuestionId(electionId, question.title)

  @SerializedName(value = "voting_method") val votingMethod: String = question.votingMethod

  @SerializedName(value = "ballot_options")
  val ballotOptions: List<String> = Collections.unmodifiableList(question.ballotOptions)

  @SerializedName(value = "write_in") val writeIn: Boolean = question.writeIn

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as ElectionQuestion
    return question == that.question && id == that.id
  }

  override fun hashCode(): Int {
    return Objects.hash(id, votingMethod, writeIn, ballotOptions, question)
  }

  override fun toString(): String {
    return "ElectionQuestion{id='$id', question='$question', votingMethod='$votingMethod', ballotOptions=${
      ballotOptions.toTypedArray().contentToString()
    }, writeIn=$writeIn}"
  }

  /**
   * This data class holds the information of an ElectionQuestion except its id.
   *
   * This is used to pack the question data when the election id is not available yet
   */
  @Immutable
  class Question(
      val title: String,
      val votingMethod: String,
      val ballotOptions: List<String>,
      val writeIn: Boolean
  ) {
    override fun equals(other: Any?): Boolean {
      if (this === other) {
        return true
      }
      if (other == null || javaClass != other.javaClass) {
        return false
      }
      val that = other as Question
      return writeIn == that.writeIn &&
          title == that.title &&
          votingMethod == that.votingMethod &&
          ballotOptions == that.ballotOptions
    }

    override fun hashCode(): Int {
      return Objects.hash(title, votingMethod, ballotOptions, writeIn)
    }

    override fun toString(): String {
      return "Question{question='$title', votingMethod='$votingMethod', ballotOptions=$ballotOptions, writeIn=$writeIn}"
    }
  }
}
