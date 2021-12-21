package com.github.dedis.popstellar.model.network.method.message.data;

import static com.github.dedis.popstellar.model.network.method.message.data.Action.ACCEPT;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.ADD;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.ADD_BROADCAST;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.CAST_VOTE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.CLOSE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.CREATE;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.ELECT;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.ELECT_ACCEPT;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.END;
import static com.github.dedis.popstellar.model.network.method.message.data.Action.LEARN;
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

import android.util.Log;

import androidx.core.util.Pair;

import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusLearn;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPrepare;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPromise;
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusPropose;
import com.github.dedis.popstellar.model.network.method.message.data.election.CastVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionEnd;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResult;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionSetup;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao;
import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting;
import com.github.dedis.popstellar.model.network.method.message.data.meeting.StateMeeting;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirpBroadcast;
import com.github.dedis.popstellar.utility.handler.ChirpHandler;
import com.github.dedis.popstellar.utility.handler.ConsensusHandler;
import com.github.dedis.popstellar.utility.handler.DataConsumer;
import com.github.dedis.popstellar.utility.handler.ElectionHandler;
import com.github.dedis.popstellar.utility.handler.LaoHandler;
import com.github.dedis.popstellar.utility.handler.RollCallHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** An abstract high level message */
public abstract class Data {

  /** A mapping of (object, action) -> (class, consumer) */
  private static final Map<EntryPair, Pair<Class<? extends Data>, DataConsumer<? extends Data>>>
      messages = buildMessagesMap();

  /**
   * Create an entry pair given obj and action
   *
   * @param obj of the pair
   * @param action of the pair
   * @return the pair
   */
  private static EntryPair pair(Objects obj, Action action) {
    return new EntryPair(obj, action);
  }

  private static <T extends Data> void add(
      Map<EntryPair, Pair<Class<? extends Data>, DataConsumer<? extends Data>>> map,
      Objects obj,
      Action action,
      Class<T> dataClass,
      DataConsumer<T> dataConsumer) {
    map.put(pair(obj, action), Pair.create(dataClass, dataConsumer));
  }

  /**
   * Build the protocol messages map
   *
   * @return the built map (Unmodifiable)
   */
  private static Map<EntryPair, Pair<Class<? extends Data>, DataConsumer<? extends Data>>>
      buildMessagesMap() {
    Map<EntryPair, Pair<Class<? extends Data>, DataConsumer<? extends Data>>> map = new HashMap<>();

    // Lao
    add(map, LAO, CREATE, CreateLao.class, LaoHandler::handleCreateLao);
    add(map, LAO, UPDATE, UpdateLao.class, LaoHandler::handleUpdateLao);
    add(map, LAO, STATE, StateLao.class, LaoHandler::handleStateLao);

    // Meeting
    add(map, MEETING, CREATE, CreateMeeting.class, null);
    add(map, MEETING, STATE, StateMeeting.class, null);

    // Message
    add(map, MESSAGE, WITNESS, WitnessMessageSignature.class, null);

    // Roll Call
    add(map, ROLL_CALL, CREATE, CreateRollCall.class, RollCallHandler::handleCreateRollCall);
    add(map, ROLL_CALL, OPEN, OpenRollCall.class, RollCallHandler::handleOpenRollCall);
    add(map, ROLL_CALL, REOPEN, OpenRollCall.class, RollCallHandler::handleOpenRollCall);
    add(map, ROLL_CALL, CLOSE, CloseRollCall.class, RollCallHandler::handleCloseRollCall);

    // Election
    add(map, ELECTION, SETUP, ElectionSetup.class, ElectionHandler::handleElectionSetup);
    add(map, ELECTION, CAST_VOTE, CastVote.class, ElectionHandler::handleCastVote);
    add(map, ELECTION, END, ElectionEnd.class, ElectionHandler::handleElectionEnd);
    add(map, ELECTION, RESULT, ElectionResult.class, ElectionHandler::handleElectionResult);

    // Consensus
    add(map, CONSENSUS, ELECT, ConsensusElect.class, ConsensusHandler::handleElect);
    add(
        map,
        CONSENSUS,
        ELECT_ACCEPT,
        ConsensusElectAccept.class,
        ConsensusHandler::handleElectAccept);
    add(map, CONSENSUS, PREPARE, ConsensusPrepare.class, ConsensusHandler::handleBackend);
    add(map, CONSENSUS, PROMISE, ConsensusPromise.class, ConsensusHandler::handleBackend);
    add(map, CONSENSUS, PROPOSE, ConsensusPropose.class, ConsensusHandler::handleBackend);
    add(map, CONSENSUS, ACCEPT, ConsensusAccept.class, ConsensusHandler::handleBackend);
    add(map, CONSENSUS, LEARN, ConsensusLearn.class, ConsensusHandler::handleLearn);

    // Social Media
    add(map, CHIRP, ADD, AddChirp.class, ChirpHandler::handleChirpAdd);
    add(map, CHIRP, ADD_BROADCAST, AddChirpBroadcast.class, null);

    return Collections.unmodifiableMap(map);
  }

  /**
   * Return the class assigned to the pair (obj, action)
   *
   * @param obj of the entry
   * @param action of the entry
   * @return the class assigned to the pair or empty if none are defined
   */
  public static Optional<Class<? extends Data>> getType(Objects obj, Action action) {
    Log.d("data", "getting data type");
    return Optional.ofNullable(messages.get(pair(obj, action))).map(p -> p.first);
  }

  /**
   * Return the data consumer assigned to the pair (obj, action)
   *
   * @param obj of the entry
   * @param action of the entry
   * @return the DataConsumer assigned to the pair or empty if none are defined
   */
  public static Optional<DataConsumer<? extends Data>> getDataConsumer(Objects obj, Action action) {
    return Optional.ofNullable(messages.get(pair(obj, action))).map(p -> p.second);
  }

  /** Returns the object the message is referring to. */
  public abstract String getObject();

  /** Returns the action the message is handling. */
  public abstract String getAction();

  /** Entry of the messages map. A pair of (Objects, Action) */
  private static final class EntryPair {

    private final Objects object;
    private final Action action;

    /**
     * Constructor for the EntryPair
     *
     * @param object of the pair
     * @param action of the pair
     */
    private EntryPair(Objects object, Action action) {
      this.object = object;
      this.action = action;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      EntryPair entryPair = (EntryPair) o;
      return object == entryPair.object && action == entryPair.action;
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(object, action);
    }
  }
}
