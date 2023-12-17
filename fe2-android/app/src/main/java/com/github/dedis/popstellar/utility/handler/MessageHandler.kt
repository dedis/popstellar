package com.github.dedis.popstellar.utility.handler

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.repository.MessageRepository
import com.github.dedis.popstellar.repository.remote.MessageSender
import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.UnknownWitnessMessageException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.handler.data.HandlerContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** General message handler class  */
@Singleton
class MessageHandler @Inject constructor(
    private val messageRepo: MessageRepository,
    private val registry: DataRegistry
) {
    /**
     * Send messages to the corresponding handler.
     *
     * @param messageSender the service used to send messages to the backend
     * @param channel the channel on which the message was received
     * @param message the message that was received
     */
    @Throws(
        DataHandlingException::class,
        UnknownLaoException::class,
        UnknownRollCallException::class,
        UnknownElectionException::class,
        NoRollCallException::class,
        UnknownWitnessMessageException::class
    )
    fun handleMessage(messageSender: MessageSender?, channel: Channel?, message: MessageGeneral) {
        val data = message.data
        val dataObj = Objects.find(data.getObject())
        val dataAction = Action.find(data.action)
        val toPersist = dataObj.hasToBePersisted()
        val toBeStored = dataAction.isStoreNeededByAction
        if (messageRepo.isMessagePresent(message.messageId, toPersist)) {
            Timber.tag(TAG)
                .d(
                    "The message with class %s has already been handled in the past",
                    data.javaClass.simpleName
                )
            return
        }
        Timber.tag(TAG)
            .d("Handling incoming message, data with class: %s", data.javaClass.simpleName)
        registry.handle(
            HandlerContext(message.messageId, message.sender, channel!!, messageSender!!),
            data,
            dataObj,
            dataAction
        )

        // Put the message in the repo
        messageRepo.addMessage(message, toBeStored, toPersist)
    }

    companion object {
        val TAG: String = MessageHandler::class.java.simpleName
    }
}