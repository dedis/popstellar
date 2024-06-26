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
  private var linkedLaos: MutableMap<String, MutableMap<String, Array<String>>> = mutableMapOf()
  private var onLinkedLaosUpdatedCallback: ((MutableMap<String, Array<String>>) -> Unit)? = null
  private var newTokensNotifyFunction: ((String, String, Array<String>) -> Unit)? = null
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

  fun addLinkedLao(laoId: String, otherLaoId: String, tokens: Array<String>) {
    if (!linkedLaos.containsKey(laoId)) {
      linkedLaos[laoId] = mutableMapOf()
    }
    linkedLaos[laoId]!![otherLaoId] = tokens
    onLinkedLaosUpdatedCallback?.invoke(linkedLaos[laoId]!!)
  }

  fun updateAndNotifyLinkedLao(
      laoId: String,
      otherLaoId: String,
      tokens: Array<String>,
      rollCallId: String
  ) {
    addLinkedLao(laoId, otherLaoId, tokens)
    newTokensNotifyFunction?.invoke(laoId, rollCallId, tokens)
  }

  fun setOnLinkedLaosUpdatedCallback(callback: (MutableMap<String, Array<String>>) -> Unit) {
    onLinkedLaosUpdatedCallback = callback
  }

  fun setNewTokensNotifyFunction(function: (String, String, Array<String>) -> Unit) {
    newTokensNotifyFunction = function
  }

  fun getLinkedLaos(laoId: String): MutableMap<String, Array<String>> {
    return if (linkedLaos.containsKey(laoId)) {
      linkedLaos[laoId]!!
    } else {
      mutableMapOf()
    }
  }

  fun flush() {
    otherLaoId = null
    otherServerAddr = null
    otherPublicKey = null
    challenge = null
    onChallengeUpdatedCallback = null
  }
}
