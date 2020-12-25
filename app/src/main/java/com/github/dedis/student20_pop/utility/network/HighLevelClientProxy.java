package com.github.dedis.student20_pop.utility.network;

import androidx.annotation.Nullable;

import com.github.dedis.student20_pop.model.Person;
import com.github.dedis.student20_pop.model.network.level.high.lao.CreateLao;
import com.github.dedis.student20_pop.model.network.level.high.lao.UpdateLao;
import com.github.dedis.student20_pop.model.network.level.high.meeting.CreateMeeting;
import com.github.dedis.student20_pop.model.network.level.high.message.WitnessMessage;
import com.github.dedis.student20_pop.model.network.level.high.rollcall.CloseRollCall;
import com.github.dedis.student20_pop.model.network.level.high.rollcall.CreateRollCall;
import com.github.dedis.student20_pop.model.network.level.high.rollcall.OpenRollCall;
import com.github.dedis.student20_pop.model.network.level.high.rollcall.ReopenRollCall;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.websocket.Session;

/**
 * A proxy of a connection to a websocket. It encapsulate the high level protocol
 */
public final class HighLevelClientProxy {

    public static final String ROOT = "/root";

    private final LowLevelClientProxy lowLevelClientProxy;
    private final String publicKey, privateKey;

    HighLevelClientProxy(Session session, Person person) {
        lowLevelClientProxy = new LowLevelClientProxy(session);
        this.publicKey = person.getId();
        this.privateKey = person.getAuthentication();
    }

    /**
     * @return the low level proxy tied to this one
     */
    public LowLevelClientProxy lowLevel() {
        return lowLevelClientProxy;
    }

    /**
     * Sends a create lao message to the back end
     *
     * @param name         of the lao
     * @param creation     time
     * @param lastModified time (should be equal to creation)
     * @param organizer    id
     * @return a CompletableFuture that will be complete once the back end responses
     */
    public CompletableFuture<Integer> createLao(String name, long creation, long lastModified, String organizer) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT,
                new CreateLao(Hash.hash(organizer, creation, name), name, creation, lastModified, organizer, new ArrayList<>()));
    }

    /**
     * Sends an update lao message to the back end
     *
     * @param laoId        id of the updated lao
     * @param name         of the lao
     * @param lastModified time
     * @param witnesses    ids of the witnesses
     * @return a CompletableFuture that will be complete once the back end responses
     */
    public CompletableFuture<Integer> updateLao(String laoId, String name, long lastModified, List<String> witnesses) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new UpdateLao(name, lastModified, witnesses));
    }

    /**
     * Sends a message as a witness to attest the validity of an other message
     *
     * @param laoId     id of the lao
     * @param messageId id of the witnessed message
     * @param data      of the message
     * @return a CompletableFuture that will be complete once the back end responses
     */
    public CompletableFuture<Integer> witnessMessage(String laoId, String messageId, String data) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new WitnessMessage(messageId, Signature.sign(privateKey, data)));
    }

    /**
     * Sends a create meeting message
     *
     * @param laoId        id of the lao
     * @param name         of the meeting
     * @param creation     time
     * @param lastModified time
     * @param location     of the meeting
     * @param start        time of the meeting
     * @param end          time of the meeting
     * @return a CompletableFuture that will be complete once the back end responses
     */
    public CompletableFuture<Integer> createMeeting(String laoId, String name, long creation, long lastModified, String location, long start, long end) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new CreateMeeting(Hash.hash("M", laoId, creation, name), name, creation, lastModified, location, start, end));
    }

    /**
     * Send a create roll call message
     *
     * @param laoId id of the lao
     * @param name of the roll call
     * @param creation time
     * @param start of the roll call.
     *              Could be immediate and therefore the startType field should be NOW.
     *              If it is in the future, startType should be SCHEDULED
     * @param startType of the roll call
     * @param location of the roll call
     *
     * @return a CompletableFuture that will be complete once the back end responses
     */
    public CompletableFuture<Integer> createRollCall(String laoId, String name, long creation, long start, CreateRollCall.StartType startType, String location) {
        return createRollCall(laoId, name, creation, start, startType, location, null);
    }

    /**
     * Send a create roll call message
     *
     * @param laoId id of the lao
     * @param name of the roll call
     * @param creation time
     * @param start of the roll call.
     *              Could be immediate and therefore the startType field should be NOW.
     *              If it is in the future, startType should be SCHEDULED
     * @param startType of the roll call
     * @param location of the roll call
     * @param description of the roll call (Optional)
     * @return a CompletableFuture that will be complete once the back end responses
     */
    public CompletableFuture<Integer> createRollCall(String laoId, String name, long creation, long start, CreateRollCall.StartType startType, String location, @Nullable String description) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new CreateRollCall(Hash.hash("R", laoId, creation, name), name, creation, start, startType, location, description));
    }

    /**
     * Send an open roll call message
     *
     * @param laoId id of the lao
     * @param rollCallId id of the roll call
     * @param start of the roll call
     * @return a CompletableFuture that will be complete once the back end responses
     */
    public CompletableFuture<Integer> openRollCall(String laoId, String rollCallId, long start) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new OpenRollCall(rollCallId, start));
    }

    /**
     * Send an reopen roll call message
     *
     * @param laoId id of the lao
     * @param rollCallId id of the roll call
     * @param start of the roll call
     * @return a CompletableFuture that will be complete once the back end responses
     */
    public CompletableFuture<Integer> reopenRollCall(String laoId, String rollCallId, long start) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new ReopenRollCall(rollCallId, start));
    }

    /**
     * Send a close roll call message
     *
     * @param laoId id of the lao
     * @param rollCallId id of the roll call
     * @param start time
     * @param end time
     * @param attendees list of scanned attendees
     * @return a CompletableFuture that will be complete once the back end responses
     */
    public CompletableFuture<Integer> closeRollCall(String laoId, String rollCallId, long start, long end, List<String> attendees) {
        return  lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new CloseRollCall(rollCallId, start, end, attendees));
    }

    /**
     * Check whether or not the connection is open or closed
     *
     * @return true if it is
     */
    public boolean isOpen() {
        return lowLevelClientProxy.getSession().isOpen();
    }
}
