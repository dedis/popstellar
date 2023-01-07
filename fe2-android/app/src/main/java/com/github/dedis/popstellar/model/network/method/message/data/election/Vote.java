package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.annotation.NonNull;

/** This interface represent a basic vote type */
public interface Vote {

  /**
   * @return the id of the question this vote is referring to
   */
  @NonNull
  String getQuestionId();

  /**
   * @return the id of the vote. Its computation depends on its type
   */
  @NonNull
  String getId();

  /**
   * @return whether or not the vote is encrypted
   */
  boolean isEncrypted();
}
