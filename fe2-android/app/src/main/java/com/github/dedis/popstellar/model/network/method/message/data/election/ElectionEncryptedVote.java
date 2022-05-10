package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;
import com.github.dedis.popstellar.model.objects.Election;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import javax.annotation.Nonnegative;

public class ElectionEncryptedVote {


  // Id of the object ElectionVote : Hash(“Vote”||election_id|| ||
  // question_id||(encrypted_vote_index(es)|encrypted_write_in))
  @SerializedName("id")
  private final String id;

  // Id of the question
  @SerializedName(value = "question")
  private final String questionId;

  // Vote array containing index corresponding to ballot options
  @SerializedName("vote")
  private final List<String> vote;



  /**
   * Constructs an encrypted vote object
   * @param questionId question ID ( SHA256('Question'||election_id||question))
   * @param voteArray Array[String]] index(es) corresponding to the ballot_options
   * @param encrytedWriteInEnabled
   * @param encrytedWriteIn string corresponding to the write_in
   */
  public ElectionEncryptedVote(
      @NonNull String questionId,
      @NonNull List<String> voteArray,
      @NonNull String encrytedWriteInEnabled,
      @NonNull String encrytedWriteIn) {

    this.questionId=questionId;
    //TO CHANGE
    this.id = "d";
    this.vote = voteArray;
  }

  @NonNull
  public List<String> getVote(){
    return vote;
  }

  @NonNull
  public String getId(){
    return id;
  }

  @NonNull
  public String getQuestionId(){
    return questionId;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getId(), getVote(), getQuestionId());
  }



}
