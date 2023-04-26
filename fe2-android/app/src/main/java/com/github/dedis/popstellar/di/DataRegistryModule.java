package com.github.dedis.popstellar.di;

import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.*;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransactionCoin;
import com.github.dedis.popstellar.model.network.method.message.data.election.*;
import com.github.dedis.popstellar.model.network.method.message.data.lao.*;
import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting;
import com.github.dedis.popstellar.model.network.method.message.data.meeting.StateMeeting;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.*;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.*;
import com.github.dedis.popstellar.utility.handler.data.*;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import static com.github.dedis.popstellar.model.network.method.message.data.Action.*;
import static com.github.dedis.popstellar.model.network.method.message.data.Objects.*;

@Module
@InstallIn(SingletonComponent.class)
public abstract class DataRegistryModule {

  private DataRegistryModule() {}

  @Provides
  @Singleton
  public static DataRegistry provideDataRegistry(
      LaoHandler laoHandler,
      RollCallHandler rollCallHandler,
      MeetingHandler meetingHandler,
      ElectionHandler electionHandler,
      ConsensusHandler consensusHandler,
      ChirpHandler chirpHandler,
      TransactionCoinHandler transactionCoinHandler) {
    DataRegistry.Builder builder = new DataRegistry.Builder();

    // Lao
    builder
        .add(LAO, CREATE, CreateLao.class, laoHandler::handleCreateLao)
        .add(LAO, UPDATE, UpdateLao.class, laoHandler::handleUpdateLao)
        .add(LAO, STATE, StateLao.class, laoHandler::handleStateLao)
        .add(LAO, GREET, GreetLao.class, laoHandler::handleGreetLao);

    // Meeting
    builder
        .add(MEETING, CREATE, CreateMeeting.class, meetingHandler::handleCreateMeeting)
        .add(MEETING, STATE, StateMeeting.class, meetingHandler::handleStateMeeting);

    // Message
    builder.add(MESSAGE, WITNESS, WitnessMessageSignature.class, null);

    // Roll Call
    builder
        .add(ROLL_CALL, CREATE, CreateRollCall.class, rollCallHandler::handleCreateRollCall)
        .add(ROLL_CALL, OPEN, OpenRollCall.class, rollCallHandler::handleOpenRollCall)
        .add(ROLL_CALL, REOPEN, OpenRollCall.class, rollCallHandler::handleOpenRollCall)
        .add(ROLL_CALL, CLOSE, CloseRollCall.class, rollCallHandler::handleCloseRollCall);

    // Election
    builder
        .add(ELECTION, SETUP, ElectionSetup.class, electionHandler::handleElectionSetup)
        .add(ELECTION, OPEN, OpenElection.class, electionHandler::handleElectionOpen)
        .add(ELECTION, CAST_VOTE, CastVote.class, electionHandler::handleCastVote)
        .add(ELECTION, END, ElectionEnd.class, electionHandler::handleElectionEnd)
        .add(ELECTION, RESULT, ElectionResult.class, electionHandler::handleElectionResult)
        .add(ELECTION, KEY, ElectionKey.class, electionHandler::handleElectionKey);

    // Consensus
    builder
        .add(CONSENSUS, ELECT, ConsensusElect.class, consensusHandler::handleElect)
        .add(
            CONSENSUS,
            ELECT_ACCEPT,
            ConsensusElectAccept.class,
            consensusHandler::handleElectAccept)
        .add(CONSENSUS, PREPARE, ConsensusPrepare.class, consensusHandler::handleBackend)
        .add(CONSENSUS, PROMISE, ConsensusPromise.class, consensusHandler::handleBackend)
        .add(CONSENSUS, PROPOSE, ConsensusPropose.class, consensusHandler::handleBackend)
        .add(CONSENSUS, ACCEPT, ConsensusAccept.class, consensusHandler::handleBackend)
        .add(CONSENSUS, LEARN, ConsensusLearn.class, consensusHandler::handleLearn)
        .add(CONSENSUS, FAILURE, ConsensusFailure.class, consensusHandler::handleConsensusFailure);

    // Social Media
    builder
        .add(CHIRP, ADD, AddChirp.class, chirpHandler::handleChirpAdd)
        .add(CHIRP, NOTIFY_ADD, NotifyAddChirp.class, null)
        .add(CHIRP, DELETE, DeleteChirp.class, chirpHandler::handleDeleteChirp)
        .add(CHIRP, NOTIFY_DELETE, NotifyDeleteChirp.class, null);

    // Digital Cash
    builder.add(
        COIN,
        POST_TRANSACTION,
        PostTransactionCoin.class,
        transactionCoinHandler::handlePostTransactionCoin);

    return builder.build();
  }
}
