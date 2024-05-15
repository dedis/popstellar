package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.network.method.message.data.federation.Challenge
import com.github.dedis.popstellar.repository.LinkedOrganizationsRepository
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import javax.inject.Inject

/** Federation messages handler class */
class LinkedOrganizationsHandler
@Inject
constructor(private val linkedOrgRepo: LinkedOrganizationsRepository) {

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
}
