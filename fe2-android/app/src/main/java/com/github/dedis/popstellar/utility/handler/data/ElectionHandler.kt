package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionKey
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionOpen
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResultQuestion
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.model.objects.Election.ElectionBuilder
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.MessageRepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.repository.database.witnessing.PendingEntity
import com.github.dedis.popstellar.utility.ActivityUtils.generateMnemonicWordFromBase64
import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.InvalidChannelException
import com.github.dedis.popstellar.utility.error.InvalidStateException
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import timber.log.Timber
import java.util.Date
import java.util.Objects
import javax.inject.Inject

/** Election messages handler class  */
class ElectionHandler @Inject constructor(
    private val messageRepo: MessageRepository,
    private val laoRepo: LAORepository,
    private val electionRepository: ElectionRepository,
    private val witnessingRepository: WitnessingRepository
) {
    /**
     * Process an ElectionSetup message.
     *
     * @param context the HandlerContext of the message
     * @param electionSetup the message that was received
     */
    @Throws(UnknownLaoException::class, InvalidChannelException::class)
    fun handleElectionSetup(context: HandlerContext, electionSetup: ElectionSetup) {
        val channel = context.channel
        val messageId = context.messageId
        val laoId = electionSetup.laoId
        if (!laoRepo.containsLao(laoId)) {
            throw UnknownLaoException(laoId)
        }
        if (!channel.isLaoChannel) {
            throw InvalidChannelException(electionSetup, "an lao channel", channel)
        }
        val laoView = laoRepo.getLaoViewByChannel(channel)
        Timber.tag(TAG)
            .d("handleElectionSetup: channel: %s, name: %s", channel, electionSetup.name)
        val election = ElectionBuilder(
            laoView.id, electionSetup.creation, electionSetup.name
        )
            .setElectionVersion(electionSetup.electionVersion)
            .setElectionQuestions(electionSetup.questions)
            .setStart(electionSetup.startTime)
            .setEnd(electionSetup.endTime)
            .setState(EventState.CREATED)
            .build()
        witnessingRepository.addWitnessMessage(
            laoId,
            electionSetupWitnessMessage(messageId, election)
        )
        if (witnessingRepository.areWitnessesEmpty(laoId)) {
            addElectionRoutine(electionRepository, election)
        } else {
            witnessingRepository.addPendingEntity(PendingEntity(messageId, laoId, election))
        }

        // Once the election is created, we subscribe to the election channel
        context
            .messageSender
            .subscribe(election.channel)
            .doOnError { err: Throwable? ->
                Timber.tag(TAG).e(err, "An error occurred while subscribing to election channel")
            }
            .onErrorComplete()
            .subscribe()
    }

    /**
     * Process an ElectionOpen message.
     *
     * @param context the HandlerContext of the message
     * @param electionOpen the message that was received
     */
    @Suppress("unused")
    @Throws(
        InvalidStateException::class,
        UnknownElectionException::class,
        UnknownLaoException::class
    )
    fun handleElectionOpen(context: HandlerContext, electionOpen: ElectionOpen) {
        val channel = context.channel
        Timber.tag(TAG).d("handleOpenElection: channel %s", channel)
        val laoId = electionOpen.laoId
        if (!laoRepo.containsLao(laoId)) {
            throw UnknownLaoException(laoId)
        }
        val election = electionRepository.getElectionByChannel(channel)

        // If the state is not created, then this message is invalid
        if (election.state != EventState.CREATED) {
            throw InvalidStateException(
                electionOpen, "election", election.state.name, EventState.CREATED.name
            )
        }

        // Sets the start time to now
        val updated =
            election.builder().setState(EventState.OPENED).setStart(electionOpen.openedAt).build()
        Timber.tag(TAG).d("election opened %d", updated.startTimestamp)
        electionRepository.updateElection(updated)
    }

    /**
     * Process an ElectionResult message.
     *
     * @param context the HandlerContext of the message
     * @param electionResult the message that was received
     */
    @Throws(UnknownElectionException::class)
    fun handleElectionResult(context: HandlerContext, electionResult: ElectionResult) {
        val channel = context.channel
        val messageId = context.messageId
        Timber.tag(TAG).d("handling election result")
        val resultsQuestions = electionResult.electionQuestionResults
        Timber.tag(TAG).d("size of resultsQuestions is %d", resultsQuestions.size)
        // No need to check here that resultsQuestions is not empty, as it is already done at the
        // creation of the ElectionResult Data
        val election = electionRepository
            .getElectionByChannel(channel)
            .builder()
            .setResults(computeResults(resultsQuestions))
            .setState(EventState.RESULTS_READY)
            .build()
        val laoId = channel.extractLaoId()
        witnessingRepository.addWitnessMessage(
            laoId, electionResultWitnessMessage(messageId, election)
        )
        if (witnessingRepository.areWitnessesEmpty(laoId)) {
            addElectionRoutine(electionRepository, election)
        } else {
            witnessingRepository.addPendingEntity(PendingEntity(messageId, laoId, election))
        }
    }

    /**
     * Process an ElectionEnd message.
     *
     * @param context the HandlerContext of the message
     * @param electionEnd the message that was received
     */
    @Suppress("unused")
    @Throws(UnknownElectionException::class)
    fun handleElectionEnd(context: HandlerContext, electionEnd: ElectionEnd?) {
        val channel = context.channel
        Timber.tag(TAG).d("handleElectionEnd: channel %s", channel)
        val election =
            electionRepository.getElectionByChannel(channel).builder().setState(EventState.CLOSED)
                .build()
        electionRepository.updateElection(election)
    }

    /**
     * Process a CastVote message.
     *
     * @param context the HandlerContext of the message
     * @param castVote the message that was received
     */
    @Throws(
        UnknownElectionException::class,
        DataHandlingException::class,
        UnknownLaoException::class
    )
    fun handleCastVote(context: HandlerContext, castVote: CastVote) {
        val channel = context.channel
        val messageId = context.messageId
        val senderPk = context.senderPk
        Timber.tag(TAG).d("handleCastVote: channel %s", channel)
        val laoId = castVote.laoId
        if (!laoRepo.containsLao(laoId)) {
            throw UnknownLaoException(laoId)
        }

        // Election id validity is checked with this
        val election = electionRepository.getElectionByChannel(channel)
        if (election.creation > castVote.creation) {
            throw DataHandlingException(castVote, "vote cannot be older than election creation")
        }

        // Verify the vote was created before the end of the election or the election is not closed yet
        if (election.endTimestamp >= castVote.creation || election.state != EventState.CLOSED) {
            // Retrieve previous cast vote message stored for the given sender
            val previousMessageId = election.messageMap[senderPk]

            // No previous message, we always handle it
            if (previousMessageId == null) {
                updateElectionWithVotes(castVote, messageId, senderPk, election)
                return
            }

            // Retrieve previous message and make sure it is a CastVote
            val previousData = messageRepo.getMessage(previousMessageId).data
                ?: throw IllegalStateException(
                    "The message corresponding to $messageId does not exist"
                )
            if (previousData !is CastVote) {
                throw DataHandlingException(
                    previousData, "The previous message of a cast vote was not a CastVote"
                )
            }

            // Verify the current cast vote message is the last one received
            if (previousData.creation <= castVote.creation) {
                updateElectionWithVotes(castVote, messageId, senderPk, election)
            }
        }
    }

    /**
     * Simple way to handle an election key, add the given key to the given election
     *
     * @param context context
     * @param electionKey key to add
     */
    @Throws(UnknownElectionException::class)
    fun handleElectionKey(context: HandlerContext, electionKey: ElectionKey) {
        val channel = context.channel
        Timber.tag(TAG).d("handleElectionKey: channel %s", channel)
        val election = electionRepository
            .getElectionByChannel(channel)
            .builder()
            .setElectionKey(electionKey.electionVoteKey)
            .build()
        electionRepository.updateElection(election)
        Timber.tag(TAG).d("handleElectionKey: election key has been set")
    }

    private fun updateElectionWithVotes(
        castVote: CastVote, messageId: MessageID?, senderPk: PublicKey?, election: Election
    ) {
        val updated = election
            .builder()
            .updateMessageMap(senderPk!!, messageId!!)
            .updateVotes(senderPk, castVote.votes)
            .build()
        electionRepository.updateElection(updated)
    }

    private fun computeResults(
        electionResultsQuestions: List<ElectionResultQuestion>
    ): Map<String, Set<QuestionResult>> {
        val results: MutableMap<String, Set<QuestionResult>> = HashMap()
        for (resultQuestion in electionResultsQuestions) {
            results[resultQuestion.id] = resultQuestion.result
        }
        return results
    }

    companion object {
        val TAG: String = ElectionHandler::class.java.simpleName

        @JvmStatic
        fun addElectionRoutine(electionRepository: ElectionRepository, election: Election?) {
            electionRepository.updateElection(election!!)
        }

        @JvmStatic
        fun electionSetupWitnessMessage(messageId: MessageID?, election: Election): WitnessMessage {
            val message = WitnessMessage(messageId)
            message.title = String.format(
                "Election %s setup at %s",
                election.name, Date(election.creationInMillis)
            )
            message.description = """
                   Mnemonic identifier :
                   ${generateMnemonicWordFromBase64(election.id, 2)}
                   
                   Opens at :
                   ${Date(election.startTimestampInMillis)}
                   
                   Closes at :
                   ${Date(election.endTimestampInMillis)}
                   
                   ${formatElectionQuestions(election.electionQuestions)}
                   """.trimIndent()
            return message
        }

        fun electionResultWitnessMessage(
            messageId: MessageID?, election: Election
        ): WitnessMessage {
            val message = WitnessMessage(messageId)
            message.title = String.format("Election %s results", election.name)
            message.description = ("""
    Mnemonic identifier :
    ${generateMnemonicWordFromBase64(election.id, 2)}
    
    Closed at :
    ${Date(election.endTimestampInMillis)}
    
    
    """.trimIndent()
                    + formatElectionResults(
                election.electionQuestions,
                election.results
            ))
            return message
        }

        private fun formatElectionQuestions(questions: List<ElectionQuestion>): String {
            val questionsDescription = StringBuilder()
            val QUESTION = "Question "
            for (i in questions.indices) {
                questionsDescription
                    .append(QUESTION)
                    .append(i + 1)
                    .append(": \n")
                    .append(questions[i].question)
                if (i < questions.size - 1) {
                    questionsDescription.append("\n\n")
                }
            }
            return questionsDescription.toString()
        }

        private fun formatElectionResults(
            questions: List<ElectionQuestion>, results: Map<String, Set<QuestionResult>>
        ): String {
            val questionsDescription = StringBuilder()
            val QUESTION = "Question "
            for (i in questions.indices) {
                questionsDescription
                    .append(QUESTION)
                    .append(i + 1)
                    .append(": \n")
                    .append(questions[i].question)
                    .append("\nResults: \n")
                val resultSet = results[questions[i].id]!!
                for (questionResult in Objects.requireNonNull(resultSet)) {
                    questionsDescription
                        .append(questionResult.ballot)
                        .append(" : ")
                        .append(questionResult.count)
                        .append("\n")
                }
                if (i < questions.size - 1) {
                    questionsDescription.append("\n\n")
                }
            }
            return questionsDescription.toString()
        }
    }
}