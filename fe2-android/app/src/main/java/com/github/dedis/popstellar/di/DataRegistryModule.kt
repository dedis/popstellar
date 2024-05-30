package com.github.dedis.popstellar.di

import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusAccept
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusFailure
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusLearn
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPrepare
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPromise
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPropose
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransactionCoin
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionKey
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionOpen
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao
import com.github.dedis.popstellar.model.network.method.message.data.lao.GreetLao
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao
import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting
import com.github.dedis.popstellar.model.network.method.message.data.meeting.StateMeeting
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddReaction
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteReaction
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.NotifyAddChirp
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.NotifyDeleteChirp
import com.github.dedis.popstellar.utility.handler.data.ChirpHandler
import com.github.dedis.popstellar.utility.handler.data.ConsensusHandler
import com.github.dedis.popstellar.utility.handler.data.ElectionHandler
import com.github.dedis.popstellar.utility.handler.data.HandlerContext
import com.github.dedis.popstellar.utility.handler.data.LaoHandler
import com.github.dedis.popstellar.utility.handler.data.MeetingHandler
import com.github.dedis.popstellar.utility.handler.data.ReactionHandler
import com.github.dedis.popstellar.utility.handler.data.RollCallHandler
import com.github.dedis.popstellar.utility.handler.data.TransactionCoinHandler
import com.github.dedis.popstellar.utility.handler.data.WitnessingHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataRegistryModule {

  @JvmStatic
  @Provides
  @Singleton
  @Suppress("LongMethod", "LongParameterList")
  fun provideDataRegistry(
      laoHandler: LaoHandler,
      rollCallHandler: RollCallHandler,
      meetingHandler: MeetingHandler,
      electionHandler: ElectionHandler,
      consensusHandler: ConsensusHandler,
      chirpHandler: ChirpHandler,
      reactionHandler: ReactionHandler,
      transactionCoinHandler: TransactionCoinHandler,
      witnessMessageHandler: WitnessingHandler
  ): DataRegistry {

    val builder = DataRegistry.Builder()

    // Lao
    builder
        .add(Objects.LAO, Action.CREATE, CreateLao::class.java) {
            context: HandlerContext,
            createLao: CreateLao ->
          laoHandler.handleCreateLao(context, createLao)
        }
        .add(Objects.LAO, Action.UPDATE, UpdateLao::class.java) {
            context: HandlerContext,
            updateLao: UpdateLao ->
          laoHandler.handleUpdateLao(context, updateLao)
        }
        .add(Objects.LAO, Action.STATE, StateLao::class.java) {
            context: HandlerContext,
            stateLao: StateLao ->
          laoHandler.handleStateLao(context, stateLao)
        }
        .add(Objects.LAO, Action.GREET, GreetLao::class.java) {
            context: HandlerContext,
            greetLao: GreetLao ->
          laoHandler.handleGreetLao(context, greetLao)
        }

    // Meeting
    builder
        .add(Objects.MEETING, Action.CREATE, CreateMeeting::class.java) {
            context: HandlerContext,
            createMeeting: CreateMeeting ->
          meetingHandler.handleCreateMeeting(context, createMeeting)
        }
        .add(Objects.MEETING, Action.STATE, StateMeeting::class.java) {
            context: HandlerContext,
            stateMeeting: StateMeeting ->
          meetingHandler.handleStateMeeting(context, stateMeeting)
        }

    // Message
    builder.add(Objects.MESSAGE, Action.WITNESS, WitnessMessageSignature::class.java) {
        context: HandlerContext,
        witnessMessageSignature: WitnessMessageSignature ->
      witnessMessageHandler.handleWitnessMessageSignature(context, witnessMessageSignature)
    }

    // Roll Call
    builder
        .add(Objects.ROLL_CALL, Action.CREATE, CreateRollCall::class.java) {
            context: HandlerContext,
            createRollCall: CreateRollCall ->
          rollCallHandler.handleCreateRollCall(context, createRollCall)
        }
        .add(Objects.ROLL_CALL, Action.OPEN, OpenRollCall::class.java) {
            context: HandlerContext,
            openRollCall: OpenRollCall ->
          rollCallHandler.handleOpenRollCall(context, openRollCall)
        }
        .add(Objects.ROLL_CALL, Action.REOPEN, OpenRollCall::class.java) {
            context: HandlerContext,
            openRollCall: OpenRollCall ->
          rollCallHandler.handleOpenRollCall(context, openRollCall)
        }
        .add(Objects.ROLL_CALL, Action.CLOSE, CloseRollCall::class.java) {
            context: HandlerContext,
            closeRollCall: CloseRollCall ->
          rollCallHandler.handleCloseRollCall(context, closeRollCall)
        }

    // Election
    builder
        .add(Objects.ELECTION, Action.SETUP, ElectionSetup::class.java) {
            context: HandlerContext,
            electionSetup: ElectionSetup ->
          electionHandler.handleElectionSetup(context, electionSetup)
        }
        .add(Objects.ELECTION, Action.OPEN, ElectionOpen::class.java) {
            context: HandlerContext,
            electionOpen: ElectionOpen ->
          electionHandler.handleElectionOpen(context, electionOpen)
        }
        .add(Objects.ELECTION, Action.CAST_VOTE, CastVote::class.java) {
            context: HandlerContext,
            castVote: CastVote ->
          electionHandler.handleCastVote(context, castVote)
        }
        .add(Objects.ELECTION, Action.END, ElectionEnd::class.java) {
            context: HandlerContext,
            electionEnd: ElectionEnd? ->
          electionHandler.handleElectionEnd(context, electionEnd)
        }
        .add(Objects.ELECTION, Action.RESULT, ElectionResult::class.java) {
            context: HandlerContext,
            electionResult: ElectionResult ->
          electionHandler.handleElectionResult(context, electionResult)
        }
        .add(Objects.ELECTION, Action.KEY, ElectionKey::class.java) {
            context: HandlerContext,
            electionKey: ElectionKey ->
          electionHandler.handleElectionKey(context, electionKey)
        }

    // Consensus
    builder
        .add(Objects.CONSENSUS, Action.ELECT, ConsensusElect::class.java) {
            context: HandlerContext,
            consensusElect: ConsensusElect ->
          consensusHandler.handleElect(context, consensusElect)
        }
        .add(Objects.CONSENSUS, Action.ELECT_ACCEPT, ConsensusElectAccept::class.java) {
            context: HandlerContext,
            consensusElectAccept: ConsensusElectAccept ->
          consensusHandler.handleElectAccept(context, consensusElectAccept)
        }
        .add(Objects.CONSENSUS, Action.PREPARE, ConsensusPrepare::class.java) {
            context: HandlerContext,
            data ->
          consensusHandler.handleBackend(context, data)
        }
        .add(Objects.CONSENSUS, Action.PROMISE, ConsensusPromise::class.java) {
            context: HandlerContext,
            data ->
          consensusHandler.handleBackend(context, data)
        }
        .add(Objects.CONSENSUS, Action.PROPOSE, ConsensusPropose::class.java) {
            context: HandlerContext,
            data ->
          consensusHandler.handleBackend(context, data)
        }
        .add(Objects.CONSENSUS, Action.ACCEPT, ConsensusAccept::class.java) {
            context: HandlerContext,
            data ->
          consensusHandler.handleBackend(context, data)
        }
        .add(Objects.CONSENSUS, Action.LEARN, ConsensusLearn::class.java) {
            context: HandlerContext,
            consensusLearn: ConsensusLearn ->
          consensusHandler.handleLearn(context, consensusLearn)
        }
        .add(Objects.CONSENSUS, Action.FAILURE, ConsensusFailure::class.java) {
            context: HandlerContext,
            failure: ConsensusFailure ->
          consensusHandler.handleConsensusFailure(context, failure)
        }

    // Social Media
    builder
        // Chirps
        .add(Objects.CHIRP, Action.ADD, AddChirp::class.java) {
            context: HandlerContext,
            addChirp: AddChirp ->
          chirpHandler.handleChirpAdd(context, addChirp)
        }
        .add(Objects.CHIRP, Action.NOTIFY_ADD, NotifyAddChirp::class.java, null)
        .add(Objects.CHIRP, Action.DELETE, DeleteChirp::class.java) {
            context: HandlerContext,
            deleteChirp: DeleteChirp ->
          chirpHandler.handleDeleteChirp(context, deleteChirp)
        }
        .add(Objects.CHIRP, Action.NOTIFY_DELETE, NotifyDeleteChirp::class.java, null)

        // Reactions
        .add(Objects.REACTION, Action.ADD, AddReaction::class.java) {
            context: HandlerContext,
            addReaction: AddReaction ->
          reactionHandler.handleAddReaction(context, addReaction)
        }
        .add(Objects.REACTION, Action.DELETE, DeleteReaction::class.java) {
            context: HandlerContext,
            deleteReaction: DeleteReaction ->
          reactionHandler.handleDeleteReaction(context, deleteReaction)
        }

    // Digital Cash
    builder.add(Objects.COIN, Action.POST_TRANSACTION, PostTransactionCoin::class.java) {
        context: HandlerContext,
        postTransactionCoin: PostTransactionCoin ->
      transactionCoinHandler.handlePostTransactionCoin(context, postTransactionCoin)
    }

    return builder.build()
  }

  /**
   * Provides a DataRegistry to catalogue data (handlers are not used)
   *
   * @return DataRegistry to be used to create a Gson
   */
  @Singleton
  fun provideDataRegistryForGson(): DataRegistry {
    val builder = DataRegistry.Builder()

    // Lao
    builder
        .add(Objects.LAO, Action.CREATE, CreateLao::class.java, null)
        .add(Objects.LAO, Action.UPDATE, UpdateLao::class.java, null)
        .add(Objects.LAO, Action.STATE, StateLao::class.java, null)
        .add(Objects.LAO, Action.GREET, GreetLao::class.java, null)

    // Meeting
    builder
        .add(Objects.MEETING, Action.CREATE, CreateMeeting::class.java, null)
        .add(Objects.MEETING, Action.STATE, StateMeeting::class.java, null)

    // Message
    builder.add(Objects.MESSAGE, Action.WITNESS, WitnessMessageSignature::class.java, null)

    // Roll Call
    builder
        .add(Objects.ROLL_CALL, Action.CREATE, CreateRollCall::class.java, null)
        .add(Objects.ROLL_CALL, Action.OPEN, OpenRollCall::class.java, null)
        .add(Objects.ROLL_CALL, Action.REOPEN, OpenRollCall::class.java, null)
        .add(Objects.ROLL_CALL, Action.CLOSE, CloseRollCall::class.java, null)

    // Election
    builder
        .add(Objects.ELECTION, Action.SETUP, ElectionSetup::class.java, null)
        .add(Objects.ELECTION, Action.OPEN, ElectionOpen::class.java, null)
        .add(Objects.ELECTION, Action.CAST_VOTE, CastVote::class.java, null)
        .add(Objects.ELECTION, Action.END, ElectionEnd::class.java, null)
        .add(Objects.ELECTION, Action.RESULT, ElectionResult::class.java, null)
        .add(Objects.ELECTION, Action.KEY, ElectionKey::class.java, null)

    // Consensus
    builder
        .add(Objects.CONSENSUS, Action.ELECT, ConsensusElect::class.java, null)
        .add(Objects.CONSENSUS, Action.ELECT_ACCEPT, ConsensusElectAccept::class.java, null)
        .add(Objects.CONSENSUS, Action.PREPARE, ConsensusPrepare::class.java, null)
        .add(Objects.CONSENSUS, Action.PROMISE, ConsensusPromise::class.java, null)
        .add(Objects.CONSENSUS, Action.PROPOSE, ConsensusPropose::class.java, null)
        .add(Objects.CONSENSUS, Action.ACCEPT, ConsensusAccept::class.java, null)
        .add(Objects.CONSENSUS, Action.LEARN, ConsensusLearn::class.java, null)
        .add(Objects.CONSENSUS, Action.FAILURE, ConsensusFailure::class.java, null)

    // Social Media
    builder
        .add(Objects.CHIRP, Action.ADD, AddChirp::class.java, null)
        .add(Objects.CHIRP, Action.NOTIFY_ADD, NotifyAddChirp::class.java, null)
        .add(Objects.CHIRP, Action.DELETE, DeleteChirp::class.java, null)
        .add(Objects.CHIRP, Action.NOTIFY_DELETE, NotifyDeleteChirp::class.java, null)
        .add(Objects.REACTION, Action.ADD, AddReaction::class.java, null)
        .add(Objects.REACTION, Action.DELETE, DeleteReaction::class.java, null)

    // Digital Cash
    builder.add(Objects.COIN, Action.POST_TRANSACTION, PostTransactionCoin::class.java, null)

    return builder.build()
  }
}
