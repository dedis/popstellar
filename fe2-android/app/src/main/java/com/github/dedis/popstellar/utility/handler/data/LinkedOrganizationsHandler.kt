package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.network.method.message.data.federation.Challenge
import com.github.dedis.popstellar.model.network.method.message.data.federation.FederationResult
import com.github.dedis.popstellar.model.network.method.message.data.federation.TokensExchange
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.LinkedOrganizationsRepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import javax.inject.Inject
import timber.log.Timber

/** Federation messages handler class */
class LinkedOrganizationsHandler
@Inject
constructor(
    private val laoRepo: LAORepository,
    private val linkedOrgRepo: LinkedOrganizationsRepository,
    private val rollCallRepo: RollCallRepository
) {

  /**
   * Process a Challenge message
   *
   * @param context the HandlerContext of the message
   * @param challenge the message that was received
   */
  @Throws(UnknownLaoException::class)
  fun handleChallenge(context: HandlerContext, challenge: Challenge) {
    linkedOrgRepo.updateChallenge(challenge)
  }

  /**
   * Process a Federation Result message
   *
   * @param context the HandlerContext of the message
   * @param result the message that was received
   */
  @Throws(UnknownLaoException::class)
  fun handleResult(context: HandlerContext, result: FederationResult) {
    if (result.isSuccess()) {
      if (result.challenge.data == linkedOrgRepo.getChallenge() &&
          linkedOrgRepo.otherLaoId != null) {
        val laoId = context.channel.extractLaoId()
        linkedOrgRepo.addLinkedLao(laoId, linkedOrgRepo.otherLaoId!!, arrayOf())
        linkedOrgRepo.addDisposable(
            context.messageSender
                .subscribe(Channel.getLaoChannel(linkedOrgRepo.otherLaoId!!))
                .subscribe(
                    { putRemoteLaoTokensInRepository(laoId) },
                    { error: Throwable -> Timber.tag(TAG).e(error, ERROR) }))
      } else {
        Timber.tag(TAG).d("Invalid FederationResult success")
      }
    } else {
      Timber.tag(TAG).d("FederationResult failure : %s", result.reason)
    }
  }

  /**
   * Process a Token Exchange message
   *
   * @param context the HandlerContext of the message
   * @param tokenExchange the message that was received
   */
  @Throws(UnknownLaoException::class)
  fun handleTokensExchange(context: HandlerContext, tokenExchange: TokensExchange) {
    // Adds the tokens in the repository
    linkedOrgRepo.addLinkedLao(
        context.channel.extractLaoId(), tokenExchange.laoId, tokenExchange.tokens)

    // Subscribes to social of the linked organization automatically
    // Note that for now the participants of an LAO automatically subscribe to social of the other
    // LAO. This might be changed in the future (making a pop-up asking the user if he/she wants
    // to subscribe to that)
    tokenExchange.tokens.forEach { t ->
      linkedOrgRepo.addDisposable(
          context.messageSender
              .subscribe(
                  Channel.getLaoChannel(tokenExchange.laoId).subChannel(SOCIAL).subChannel(t))
              .subscribe(
                  { Timber.tag(TAG).d(SUCCESS) },
                  { error: Throwable -> Timber.tag(TAG).e(error, ERROR) }))
    }
    linkedOrgRepo.addDisposable(
        context.messageSender
            .subscribe(
                Channel.getLaoChannel(tokenExchange.laoId).subChannel(SOCIAL).subChannel(REACTIONS))
            .subscribe(
                { Timber.tag(TAG).d(SUCCESS) },
                { error: Throwable -> Timber.tag(TAG).e(error, ERROR) }))
  }

  private fun putRemoteLaoTokensInRepository(myLaoId: String) {
    try {
      val rollCall = rollCallRepo.getLastClosedRollCall(linkedOrgRepo.otherLaoId!!)
      val attendees = rollCall.attendees.map { e -> e.encoded }.toTypedArray()
      linkedOrgRepo.updateAndNotifyLinkedLao(
          myLaoId, linkedOrgRepo.otherLaoId!!, attendees, rollCall.persistentId)
    } catch (e: NoRollCallException) {
      Timber.tag(TAG).d("No RollCall was found on the linked LAO")
    }
  }

  companion object {
    private val TAG = LinkedOrganizationsHandler::class.java.simpleName
    private const val SOCIAL = "social"
    private const val REACTIONS = "reactions"
    private const val SUCCESS = "subscription is a success"
    private const val ERROR = "subscription error"
  }
}
