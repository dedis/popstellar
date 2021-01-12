package com.github.dedis.student20_pop.utility.protocol;

import androidx.annotation.Nullable;

import com.github.dedis.student20_pop.model.network.query.data.rollcall.CreateRollCall;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface HighLevelProxy extends Closeable {

    String ROOT = "/root";

    /**
     * @return the low level proxy tied to this one
     */
    LowLevelProxy lowLevel();

    /**
     * Sends a create lao message to the back end
     *
     * @param name         of the lao
     * @param creation     time
     * @param organizer    id
     * @return a CompletableFuture that will be complete once the back end responses
     */
    CompletableFuture<Integer> createLao(String name, long creation, String organizer);

    /**
     * Sends an update lao message to the back end
     *
     * @param laoId        id of the updated lao
     * @param organizer    public key of the organizer
     * @param name         of the lao
     * @param lastModified time
     * @param witnesses    ids of the witnesses
     * @return a CompletableFuture that will be complete once the back end responses
     */
    CompletableFuture<Integer> updateLao(String laoId, String organizer, String name, long lastModified, List<String> witnesses);

    /**
     * Sends a message as a witness to attest the validity of an other message
     *
     * @param laoId     id of the lao
     * @param messageId id of the witnessed message
     * @param data      of the message
     * @return a CompletableFuture that will be complete once the back end responses
     */
    CompletableFuture<Integer> witnessMessage(String laoId, String messageId, String data);

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
    CompletableFuture<Integer> createMeeting(String laoId, String name, long creation, long lastModified, String location, long start, long end);

    /**
     * Send a create roll call message
     *
     * @param laoId     id of the lao
     * @param name      of the roll call
     * @param creation  time
     * @param start     of the roll call.
     *                  Could be immediate and therefore the startType field should be NOW.
     *                  If it is in the future, startType should be SCHEDULED
     * @param startType of the roll call
     * @param location  of the roll call
     * @return a CompletableFuture that will be complete once the back end responses
     */
    CompletableFuture<Integer> createRollCall(String laoId, String name, long creation, long start, CreateRollCall.StartType startType, String location);

    /**
     * Send a create roll call message
     *
     * @param laoId       id of the lao
     * @param name        of the roll call
     * @param creation    time
     * @param start       of the roll call.
     *                    Could be immediate and therefore the startType field should be NOW.
     *                    If it is in the future, startType should be SCHEDULED
     * @param startType   of the roll call
     * @param location    of the roll call
     * @param description of the roll call (Optional)
     * @return a CompletableFuture that will be complete once the back end responses
     */
    CompletableFuture<Integer> createRollCall(String laoId, String name, long creation, long start, CreateRollCall.StartType startType, String location, @Nullable String description);

    /**
     * Send an open roll call message
     *
     * @param laoId      id of the lao
     * @param rollCallId id of the roll call
     * @param start      of the roll call
     * @return a CompletableFuture that will be complete once the back end responses
     */
    CompletableFuture<Integer> openRollCall(String laoId, String rollCallId, long start);

    /**
     * Send a close roll call message
     *
     * @param laoId      id of the lao
     * @param rollCallId id of the roll call
     * @param start      time
     * @param end        time
     * @param attendees  list of scanned attendees
     * @return a CompletableFuture that will be complete once the back end responses
     */
    CompletableFuture<Integer> closeRollCall(String laoId, String rollCallId, long start, long end, List<String> attendees);

    /**
     * Close the proxy for the given reason.
     *
     * @param reason of the closing
     */
    void close(Throwable reason);
}
