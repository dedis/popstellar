package com.github.dedis.student20_pop.utility.network;

import com.github.dedis.student20_pop.model.Person;
import com.github.dedis.student20_pop.model.network.level.high.lao.CreateLao;
import com.github.dedis.student20_pop.model.network.level.high.lao.UpdateLao;
import com.github.dedis.student20_pop.model.network.level.high.meeting.CreateMeeting;
import com.github.dedis.student20_pop.model.network.level.high.message.WitnessMessage;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A proxy of a connection to a websocket. It encapsulate the high level protocol
 */
public final class HighLevelClientProxy implements Closeable {

    public static final String ROOT = "/root";

    private final LowLevelClientProxy lowLevelClientProxy;
    private final String publicKey, privateKey;

    public HighLevelClientProxy(Person owner, LowLevelClientProxy lowLevelClientProxy) {
        this.publicKey = owner.getId();
        this.privateKey = owner.getAuthentication();
        this.lowLevelClientProxy = lowLevelClientProxy;
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
                new CreateLao(Hash.hash(organizer + creation + name), name, creation, lastModified, organizer, new ArrayList<>()));
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
                new CreateMeeting(Hash.hash(laoId + creation + name), name, creation, lastModified, location, start, end));
    }

    @Override
    public void close() {
        lowLevelClientProxy.close();
    }
}
