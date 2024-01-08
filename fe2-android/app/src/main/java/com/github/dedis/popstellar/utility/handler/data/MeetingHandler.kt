package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting
import com.github.dedis.popstellar.model.network.method.message.data.meeting.StateMeeting
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.event.MeetingBuilder
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.MeetingRepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.repository.database.witnessing.PendingEntity
import com.github.dedis.popstellar.utility.GeneralUtils.generateMnemonicWordFromBase64
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import java.util.Date
import javax.inject.Inject
import timber.log.Timber

class MeetingHandler
@Inject
constructor(
    private val laoRepo: LAORepository,
    private val meetingRepo: MeetingRepository,
    private val witnessingRepo: WitnessingRepository
) {

  /**
   * Process a CreateMeeting message.
   *
   * @param context the HandlerContext of the message
   * @param createMeeting the message that was received
   */
  @Throws(UnknownLaoException::class)
  fun handleCreateMeeting(context: HandlerContext, createMeeting: CreateMeeting) {
    val channel = context.channel
    val messageId = context.messageId
    Timber.tag(TAG).d("handleCreateMeeting: channel: %s, name: %s", channel, createMeeting.name)

    val laoView = laoRepo.getLaoViewByChannel(channel)
    val laoId = laoView.id
    val meeting =
        MeetingBuilder()
            .setId(createMeeting.id)
            .setCreation(createMeeting.creation)
            .setStart(createMeeting.start)
            .setEnd(createMeeting.end)
            .setName(createMeeting.name)
            .setLocation(createMeeting.location.orElse(""))
            .setLastModified(createMeeting.creation)
            .setModificationId("")
            .setModificationSignatures(ArrayList())
            .build()

    witnessingRepo.addWitnessMessage(laoView.id, createMeetingWitnessMessage(messageId, meeting))
    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      addMeetingRoutine(meetingRepo, laoId, meeting)
    } else {
      witnessingRepo.addPendingEntity(PendingEntity(messageId, laoId, meeting))
    }
  }

  @Throws(UnknownLaoException::class)
  fun handleStateMeeting(context: HandlerContext, stateMeeting: StateMeeting) {
    val channel = context.channel
    val messageId = context.messageId
    Timber.tag(TAG).d("handleStateMeeting: channel: %s, name: %s", channel, stateMeeting.name)

    val laoView = laoRepo.getLaoViewByChannel(channel)
    val laoId = laoView.id
    // TODO: modify with the right logic when implementing the state functionality
    val meeting =
        MeetingBuilder()
            .setId(stateMeeting.id)
            .setCreation(stateMeeting.creation)
            .setStart(stateMeeting.start)
            .setEnd(stateMeeting.end)
            .setName(stateMeeting.name)
            .setLocation(stateMeeting.location.orElse(""))
            .setLastModified(stateMeeting.creation)
            .setModificationId(stateMeeting.modificationId)
            .setModificationSignatures(stateMeeting.modificationSignatures)
            .build()

    witnessingRepo.addWitnessMessage(laoView.id, stateMeetingWitnessMessage(messageId, meeting))
    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      addMeetingRoutine(meetingRepo, laoId, meeting)
    } else {
      witnessingRepo.addPendingEntity(PendingEntity(messageId, laoId, meeting))
    }
  }

  companion object {
    private val TAG = RollCallHandler::class.java.simpleName

    @JvmStatic
    fun addMeetingRoutine(meetingRepository: MeetingRepository, laoId: String, meeting: Meeting) {
      meetingRepository.updateMeeting(laoId, meeting)
    }

    @JvmStatic
    fun createMeetingWitnessMessage(messageId: MessageID, meeting: Meeting): WitnessMessage {
      val message = WitnessMessage(messageId)

      message.title = "The Meeting ${meeting.name} was created at ${Date(meeting.creation * 1000)}"
      message.description =
          "Mnemonic identifier :\n${generateMnemonicWordFromBase64(meeting.id, 2)}\n\n" +
              (if (meeting.location.isEmpty()) "" else "Location :\n${meeting.location}\n\n}") +
              "Starts at :\n${Date(meeting.startTimestampInMillis)}\n\n" +
              "Finishes at :\n${Date(meeting.endTimestampInMillis)}"

      return message
    }

    fun stateMeetingWitnessMessage(messageId: MessageID, meeting: Meeting): WitnessMessage {
      val message = WitnessMessage(messageId)

      message.title =
          "The Meeting ${meeting.name} was modified at ${Date(meeting.lastModified * 1000)}"
      message.description =
          "Mnemonic identifier :\n${generateMnemonicWordFromBase64(meeting.id, 2)}\n\n" +
              (if (meeting.location.isEmpty()) "" else "Location :\n${meeting.location}\n\n}") +
              "Starts at :\n${Date(meeting.startTimestampInMillis)}\n\n" +
              "Finishes at :\n${Date(meeting.endTimestampInMillis)}"

      return message
    }
  }
}
