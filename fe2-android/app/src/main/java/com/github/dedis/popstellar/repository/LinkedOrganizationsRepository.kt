package com.github.dedis.popstellar.repository

import android.app.Activity
import android.app.Application
import androidx.lifecycle.Lifecycle
import com.github.dedis.popstellar.model.network.method.message.data.federation.Challenge
import com.github.dedis.popstellar.utility.GeneralUtils
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.util.EnumMap
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class is the repository of federation
 *
 * Its main purpose is to store received messages
 */
@Singleton
class LinkedOrganizationsRepository @Inject constructor(application: Application) {
  private var challenge: Challenge? = null
  private var onChallengeUpdatedCallback: ((Challenge) -> Unit)? = null
  private var linkedLaos: MutableMap<String, MutableMap<String, Array<String>>> = mutableMapOf()
  private var onLinkedLaosUpdatedCallback: ((String, MutableMap<String, Array<String>>) -> Unit)? =
      null
  private var newTokensNotifyFunction: ((String, String, String, Array<String>) -> Unit)? = null
  private val disposables = CompositeDisposable()
  var otherLaoId: String? = null
  var otherServerAddr: String? = null
  var otherPublicKey: String? = null

  init {
    val consumerMap: MutableMap<Lifecycle.Event, Consumer<Activity>> =
        EnumMap(Lifecycle.Event::class.java)
    consumerMap[Lifecycle.Event.ON_STOP] = Consumer { disposables.clear() }
    application.registerActivityLifecycleCallbacks(GeneralUtils.buildLifecycleCallback(consumerMap))
  }

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
    val laoMap = linkedLaos.getOrPut(laoId) { mutableMapOf() }
    laoMap[otherLaoId] = tokens
    onLinkedLaosUpdatedCallback?.invoke(laoId, laoMap)
  }

  fun updateAndNotifyLinkedLao(
      laoId: String,
      otherLaoId: String,
      tokens: Array<String>,
      rollCallId: String
  ) {
    addLinkedLao(laoId, otherLaoId, tokens)
    newTokensNotifyFunction?.invoke(laoId, otherLaoId, rollCallId, tokens)
  }

  fun setOnLinkedLaosUpdatedCallback(
      callback: (String, MutableMap<String, Array<String>>) -> Unit
  ) {
    onLinkedLaosUpdatedCallback = callback
  }

  fun setNewTokensNotifyFunction(function: (String, String, String, Array<String>) -> Unit) {
    newTokensNotifyFunction = function
  }

  fun getLinkedLaos(laoId: String): MutableMap<String, Array<String>> {
    return linkedLaos.getOrDefault(laoId, mutableMapOf())
  }

  fun addDisposable(disposable: Disposable) {
    disposables.add(disposable)
  }

  fun flush() {
    otherLaoId = null
    otherServerAddr = null
    otherPublicKey = null
    challenge = null
    onChallengeUpdatedCallback = null
  }
}
