package com.github.dedis.popstellar.di;

import static com.github.dedis.popstellar.model.network.method.message.data.Action.ACCEPT;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.ADD;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.CAST_VOTE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.CLOSE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.CREATE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.DELETE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.ELECT;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.ELECT_ACCEPT;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.END;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.FAILURE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.GREET;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.KEY;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.LEARN;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.NOTIFY_ADD;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.NOTIFY_DELETE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.OPEN;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.PREPARE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.PROMISE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.PROPOSE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.REOPEN;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.RESULT;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.SETUP;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.STATE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.UPDATE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.WITNESS;
import static com.github.dedis.popstellar.model.network.method.message.data.Objects.CHIRP;
import static com.github.dedis.popstellar.model.network.method.message.data.Objects.CONSENSUS;
import static com.github.dedis.popstellar.model.network.method.message.data.Objects.ELECTION;
import static com.github.dedis.popstellar.model.network.method.message.data.Objects.LAO;
import static com.github.dedis.popstellar.model.network.method.message.data.Objects.MEETING;
import static com.github.dedis.popstellar.model.network.method.message.data.Objects.MESSAGE;
import static com.github.dedis.popstellar.model.network.method.message.data.Objects.ROLL_CALL;

import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusFailure;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusLearn;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPrepare;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPromise;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPropose;
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionKey;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.popstellar.model.network.method.message.data.election.OpenElection;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.GreetLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting;
import com.github.dedis.popstellar.model.network.method.message.data.meeting.StateMeeting;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.NotifyAddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.NotifyDeleteChirp;
import com.github.dedis.popstellar.utility.handler.data.ChirpHandler;
import com.github.dedis.popstellar.utility.handler.data.ConsensusHandler;
import com.github.dedis.popstellar.utility.handler.data.ElectionHandler;
import com.github.dedis.popstellar.utility.handler.data.LaoHandler;
import com.github.dedis.popstellar.utility.handler.data.RollCallHandler;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

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

    return builder.build();
  }
}
