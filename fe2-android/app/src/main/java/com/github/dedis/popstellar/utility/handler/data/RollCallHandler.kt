package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.event.RollCallBuilder
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.repository.DigitalCashRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.repository.database.witnessing.PendingEntity
import com.github.dedis.popstellar.utility.ActivityUtils.generateMnemonicWordFromBase64
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import java.util.Date
import java.util.function.Consumer
import javax.inject.Inject
import timber.log.Timber

/** Roll Call messages handler class */
class RollCallHandler
@Inject
constructor(
    private val laoRepo: LAORepository,
    private val rollCallRepo: RollCallRepository,
    private val digitalCashRepo: DigitalCashRepository,
    private val witnessingRepo: WitnessingRepository
) {
  /**
   * Process a CreateRollCall message.
   *
   * @param context the HandlerContext of the message
   * @param createRollCall the message that was received
   */
  @Throws(UnknownLaoException::class)
  fun handleCreateRollCall(context: HandlerContext, createRollCall: CreateRollCall) {
    val channel = context.channel
    val messageId = context.messageId
    Timber.tag(TAG).d("handleCreateRollCall: channel: %s, name: %s", channel, createRollCall.name)
    val laoView = laoRepo.getLaoViewByChannel(channel)
    val builder = RollCallBuilder()
    builder
        .setId(createRollCall.id)
        .setPersistentId(createRollCall.id)
        .setCreation(createRollCall.creation)
        .setState(EventState.CREATED)
        .setStart(createRollCall.proposedStart)
        .setEnd(createRollCall.proposedEnd)
        .setName(createRollCall.name)
        .setLocation(createRollCall.location)
        .setDescription(createRollCall.description.orElse(""))
        .setEmptyAttendees()
    val laoId = laoView.id
    val rollCall = builder.build()
    witnessingRepo.addWitnessMessage(laoId, createRollCallWitnessMessage(messageId, rollCall))
    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      addRollCallRoutine(rollCallRepo, digitalCashRepo, laoId, rollCall)
    } else {
      // Update the repo with the created rollcall when the witness policy is satisfied
      witnessingRepo.addPendingEntity(PendingEntity(messageId, laoId, rollCall))
    }
  }

  /**
   * Process an OpenRollCall message.
   *
   * @param context the HandlerContext of the message
   * @param openRollCall the message that was received
   */
  @Throws(UnknownLaoException::class, UnknownRollCallException::class)
  fun handleOpenRollCall(context: HandlerContext, openRollCall: OpenRollCall) {
    val channel = context.channel
    val messageId = context.messageId
    Timber.tag(TAG).d("handleOpenRollCall: channel: %s, msg: %s", channel, openRollCall)
    val laoView = laoRepo.getLaoViewByChannel(channel)
    val updateId = openRollCall.updateId
    val opens = openRollCall.opens
    val existingRollCall = rollCallRepo.getRollCallWithId(laoView.id, opens)
    val builder = RollCallBuilder()
    builder
        .setId(updateId)
        .setPersistentId(existingRollCall.persistentId)
        .setCreation(existingRollCall.creation)
        .setState(EventState.OPENED)
        .setStart(openRollCall.openedAt)
        .setEnd(existingRollCall.end)
        .setName(existingRollCall.name)
        .setLocation(existingRollCall.location)
        .setDescription(existingRollCall.description)
        .setEmptyAttendees()
    val laoId = laoView.id
    val rollCall = builder.build()
    witnessingRepo.addWitnessMessage(laoId, openRollCallWitnessMessage(messageId, rollCall))
    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      addRollCallRoutine(rollCallRepo, digitalCashRepo, laoId, rollCall)
    } else {
      // Update the repo with the created rollcall when the witness policy is satisfied
      witnessingRepo.addPendingEntity(PendingEntity(messageId, laoId, rollCall))
    }
  }

  /**
   * Process a CloseRollCall message.
   *
   * @param context the HandlerContext of the message
   * @param closeRollCall the message that was received
   */
  @Throws(UnknownLaoException::class, UnknownRollCallException::class)
  fun handleCloseRollCall(context: HandlerContext, closeRollCall: CloseRollCall) {
    val channel = context.channel
    val messageId = context.messageId
    Timber.tag(TAG).d("handleCloseRollCall: channel: %s", channel)
    val laoView = laoRepo.getLaoViewByChannel(channel)
    val updateId = closeRollCall.updateId
    val closes = closeRollCall.closes
    val existingRollCall = rollCallRepo.getRollCallWithId(laoView.id, closes)
    val currentAttendees = existingRollCall.attendees
    currentAttendees.addAll(closeRollCall.attendees)
    val builder = RollCallBuilder()
    builder
        .setId(updateId)
        .setPersistentId(existingRollCall.persistentId)
        .setCreation(existingRollCall.creation)
        .setState(EventState.CLOSED)
        .setStart(existingRollCall.start)
        .setName(existingRollCall.name)
        .setLocation(existingRollCall.location)
        .setDescription(existingRollCall.description)
        .setAttendees(currentAttendees)
        .setEnd(closeRollCall.closedAt)
    val laoId = laoView.id
    val rollCall = builder.build()
    witnessingRepo.addWitnessMessage(laoId, closeRollCallWitnessMessage(messageId, rollCall))
    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      addRollCallRoutine(rollCallRepo, digitalCashRepo, laoId, rollCall)
    } else {
      // Update the repo with the closed rollcall when the witness policy is
      // satisfied and apply a specific routine
      witnessingRepo.addPendingEntity(PendingEntity(messageId, laoId, rollCall))
    }

    // Subscribe to the social media channels
    // (this is not the expected behavior as users should be able to choose who to subscribe to. But
    // as this part is not implemented, currently, it subscribes to everyone)
    rollCall.attendees.forEach(
        Consumer { token: PublicKey ->
          rollCallRepo.addDisposable(
              context.messageSender
                  .subscribe(channel.subChannel("social").subChannel(token.encoded))
                  .subscribe({ Timber.tag(TAG).d("subscription a success") }) { error: Throwable? ->
                    Timber.tag(TAG).e(error, "subscription error")
                  })
        })

    // Subscribe to reactions
    rollCallRepo.addDisposable(
        context.messageSender
            .subscribe(channel.subChannel("social").subChannel("reactions"))
            .subscribe({ Timber.tag(TAG).d("subscription a success") }) { error: Throwable? ->
              Timber.tag(TAG).e(error, "subscription error")
            })
  }

  companion object {
    private val TAG = RollCallHandler::class.java.simpleName
    private const val MNEMONIC_STRING = "Mnemonic identifier :\n"
    private const val LOCATION_STRING = "Location :\n"
    private const val DESCRIPTION_STRING = "Description :\n"

    @JvmStatic
    fun addRollCallRoutine(
        rollCallRepo: RollCallRepository,
        digitalCashRepo: DigitalCashRepository,
        laoId: String,
        rollCall: RollCall
    ) {
      rollCallRepo.updateRollCall(laoId, rollCall)
      if (rollCall.isClosed) {
        digitalCashRepo.initializeDigitalCash(laoId, ArrayList(rollCall.attendees))
      }
    }

    @JvmStatic
    fun createRollCallWitnessMessage(messageId: MessageID?, rollCall: RollCall): WitnessMessage {
      val message = WitnessMessage(messageId)
      message.title =
          String.format(
              "The Roll Call %s was created at %s", rollCall.name, Date(rollCall.creation * 1000))
      message.description =
          """
                   $MNEMONIC_STRING${generateMnemonicWordFromBase64(rollCall.persistentId, 2)}
                   
                   $LOCATION_STRING${rollCall.location}
                   
                   ${
        if (rollCall.description.isEmpty()) "" else """
     $DESCRIPTION_STRING${rollCall.description}
     
     
     """.trimIndent()
      }Opens at :
                   ${Date(rollCall.startTimestampInMillis)}
                   
                   Closes at :
                   ${Date(rollCall.endTimestampInMillis)}
                   """
              .trimIndent()
      return message
    }

    @JvmStatic
    fun openRollCallWitnessMessage(messageId: MessageID?, rollCall: RollCall): WitnessMessage {
      val message = WitnessMessage(messageId)
      message.title =
          String.format(
              "The Roll Call %s was opened at %s",
              rollCall.name,
              Date(rollCall.startTimestampInMillis))
      message.description =
          """
                   $MNEMONIC_STRING${generateMnemonicWordFromBase64(rollCall.persistentId, 2)}
                   
                   $LOCATION_STRING${rollCall.location}
                   
                   ${
        if (rollCall.description.isEmpty()) "" else """
     $DESCRIPTION_STRING${rollCall.description}
     
     
     """.trimIndent()
      }Closes at :
                   ${Date(rollCall.endTimestampInMillis)}
                   """
              .trimIndent()
      return message
    }

    @JvmStatic
    fun closeRollCallWitnessMessage(messageId: MessageID?, rollCall: RollCall): WitnessMessage {
      val message = WitnessMessage(messageId)
      message.title =
          String.format(
              "The Roll Call %s was closed at %s",
              rollCall.name,
              Date(rollCall.endTimestampInMillis))
      message.description =
          """
                   $MNEMONIC_STRING${generateMnemonicWordFromBase64(rollCall.persistentId, 2)}
                   
                   $LOCATION_STRING${rollCall.location}
                   
                   ${
        if (rollCall.description.isEmpty()) "" else """
     $DESCRIPTION_STRING${rollCall.description}
     
     
     """.trimIndent()
      }${formatAttendees(rollCall.attendees)}
                   """
              .trimIndent()
      return message
    }

    private fun formatAttendees(attendees: Set<PublicKey>): String {
      val stringBuilder = StringBuilder("Attendees :")
      var index = 1
      for (attendee in attendees) {
        stringBuilder.append("\n").append(index).append(") ").append(attendee.encoded)
        ++index
      }
      return stringBuilder.toString()
    }
  }
}
