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
  var otherLaoId: String? = null
  var otherServerAddr: String? = null
  var otherPublicKey: String? = null

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

  fun getChallenge(): Challenge? {
    return challenge
  }

  fun flush() {
    otherLaoId = null
    otherServerAddr = null
    otherPublicKey = null
    challenge = null
    onChallengeUpdatedCallback = null
  }
}
