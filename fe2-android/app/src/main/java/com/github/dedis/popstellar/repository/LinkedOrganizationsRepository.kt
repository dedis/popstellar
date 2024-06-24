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
  private var linkedLaos: MutableMap<String, Array<String>> = mutableMapOf()
  private var onLinkedLaosUpdatedCallback: ((MutableMap<String, Array<String>>) -> Unit)? = null
  var otherLaoId: String? = null
  var otherServerAddr: String? = null
  var otherPublicKey: String? = null

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

  fun addLinkedLao(lao_id: String, tokens: Array<String>) {
    linkedLaos[lao_id] = tokens
    onLinkedLaosUpdatedCallback?.invoke(linkedLaos)
  }

  fun setOnLinkedLaosUpdatedCallback(callback: (MutableMap<String, Array<String>>) -> Unit) {
    onLinkedLaosUpdatedCallback = callback
  }

  fun getLinkedLaos(): MutableMap<String, Array<String>> {
    return linkedLaos
  }

  fun flush() {
    otherLaoId = null
    otherServerAddr = null
    otherPublicKey = null
    challenge = null
    onChallengeUpdatedCallback = null
  }
}
