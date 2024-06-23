package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.network.method.message.data.federation.Challenge
import com.github.dedis.popstellar.model.network.method.message.data.federation.FederationResult
import com.github.dedis.popstellar.model.network.method.message.data.federation.TokensExchange
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.LinkedOrganizationsRepository
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import javax.inject.Inject
import timber.log.Timber

/** Federation messages handler class */
class LinkedOrganizationsHandler
@Inject
constructor(
    private val laoRepo: LAORepository,
    private val linkedOrgRepo: LinkedOrganizationsRepository
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
      // DO STUFF
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
    linkedOrgRepo.addLinkedLao(tokenExchange.laoId, tokenExchange.tokens)
    tokenExchange.tokens.forEach { t ->
      laoRepo.addDisposable(
          context.messageSender
              .subscribe(
                  Channel.getLaoChannel(tokenExchange.laoId).subChannel("social").subChannel(t))
              .subscribe(
                  { Timber.tag(TAG).d("subscription a success") },
                  { error: Throwable -> Timber.tag(TAG).e(error, "subscription error") }))
    }
  }

  companion object {
    private val TAG = LinkedOrganizationsHandler::class.java.simpleName
  }
}
