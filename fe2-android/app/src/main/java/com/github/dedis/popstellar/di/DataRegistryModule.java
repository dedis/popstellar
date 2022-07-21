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
  public static DataRegistry provideDataRegistry() {
    DataRegistry.Builder builder = new DataRegistry.Builder();

    // Lao
    builder
        .add(LAO, CREATE, CreateLao.class, LaoHandler::handleCreateLao)
        .add(LAO, UPDATE, UpdateLao.class, LaoHandler::handleUpdateLao)
        .add(LAO, STATE, StateLao.class, LaoHandler::handleStateLao)
        .add(LAO, GREET, GreetLao.class, LaoHandler::handleGreetLao);

    // Meeting
    builder
        .add(MEETING, CREATE, CreateMeeting.class, null)
        .add(MEETING, STATE, StateMeeting.class, null);

    // Message
    builder.add(MESSAGE, WITNESS, WitnessMessageSignature.class, null);

    // Roll Call
    builder
        .add(ROLL_CALL, CREATE, CreateRollCall.class, RollCallHandler::handleCreateRollCall)
        .add(ROLL_CALL, OPEN, OpenRollCall.class, RollCallHandler::handleOpenRollCall)
        .add(ROLL_CALL, REOPEN, OpenRollCall.class, RollCallHandler::handleOpenRollCall)
        .add(ROLL_CALL, CLOSE, CloseRollCall.class, RollCallHandler::handleCloseRollCall);

    // Election
    builder
        .add(ELECTION, SETUP, ElectionSetup.class, ElectionHandler::handleElectionSetup)
        .add(ELECTION, OPEN, OpenElection.class, ElectionHandler::handleElectionOpen)
        .add(ELECTION, CAST_VOTE, CastVote.class, ElectionHandler::handleCastVote)
        .add(ELECTION, END, ElectionEnd.class, ElectionHandler::handleElectionEnd)
        .add(ELECTION, RESULT, ElectionResult.class, ElectionHandler::handleElectionResult)
        .add(ELECTION, KEY, ElectionKey.class, ElectionHandler::handleElectionKey);

    // Consensus
    builder
        .add(CONSENSUS, ELECT, ConsensusElect.class, ConsensusHandler::handleElect)
        .add(
            CONSENSUS,
            ELECT_ACCEPT,
            ConsensusElectAccept.class,
            ConsensusHandler::handleElectAccept)
        .add(CONSENSUS, PREPARE, ConsensusPrepare.class, ConsensusHandler::handleBackend)
        .add(CONSENSUS, PROMISE, ConsensusPromise.class, ConsensusHandler::handleBackend)
        .add(CONSENSUS, PROPOSE, ConsensusPropose.class, ConsensusHandler::handleBackend)
        .add(CONSENSUS, ACCEPT, ConsensusAccept.class, ConsensusHandler::handleBackend)
        .add(CONSENSUS, LEARN, ConsensusLearn.class, ConsensusHandler::handleLearn)
        .add(CONSENSUS, FAILURE, ConsensusFailure.class, ConsensusHandler::handleConsensusFailure);

    // Social Media
    builder
        .add(CHIRP, ADD, AddChirp.class, ChirpHandler::handleChirpAdd)
        .add(CHIRP, NOTIFY_ADD, NotifyAddChirp.class, null)
        .add(CHIRP, DELETE, DeleteChirp.class, ChirpHandler::handleDeleteChirp)
        .add(CHIRP, NOTIFY_DELETE, NotifyDeleteChirp.class, null);

    // Digital Cash
    builder.add(
        COIN,
        POST_TRANSACTION,
        PostTransactionCoin.class,
        TransactionCoinHandler::handlePostTransactionCoin);

    return builder.build();
  }
}
