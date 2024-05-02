package com.github.dedis.popstellar.repository

import com.github.dedis.popstellar.model.network.method.message.data.federation.Challenge
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class is the repository of federation
 *
 * Its main purpose is to store received messages
 */
@Singleton
class LinkedOrganizationsRepository @Inject constructor() {
  private var challenge: Challenge? = null
  private var onChallengeUpdatedCallback: ((Challenge) -> Unit)? = null

  /**
   * Updates the challenge
   *
   * @param challenge the new Challenge
   */
  fun updateChallenge(challenge: Challenge) {
    this.challenge = challenge
    onChallengeUpdatedCallback?.invoke(challenge)
  }

  fun setOnChallengeUpdatedCallback(callback: (Challenge) -> Unit) {
    onChallengeUpdatedCallback = callback
  }
}
