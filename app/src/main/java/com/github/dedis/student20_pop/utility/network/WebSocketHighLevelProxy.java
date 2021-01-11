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
import com.github.dedis.student20_pop.utility.protocol.HighLevelProxy;
import com.github.dedis.student20_pop.utility.protocol.LowLevelProxy;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A proxy of a connection to a websocket. It encapsulate the high level protocol
 */
public final class WebSocketHighLevelProxy implements HighLevelProxy {

    private final LowLevelProxy lowLevelClientProxy;
    private final String publicKey, privateKey;

    public WebSocketHighLevelProxy(Person owner, LowLevelProxy lowLevelClientProxy) {
        this.publicKey = owner.getId();
        this.privateKey = owner.getAuthentication();
        this.lowLevelClientProxy = lowLevelClientProxy;
    }

    @Override
    public LowLevelProxy lowLevel() {
        return lowLevelClientProxy;
    }

    @Override
    public CompletableFuture<Integer> createLao(String name, long creation, long lastModified, String organizer) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT,
                new CreateLao(Hash.hash(organizer, creation, name), name, creation, lastModified, organizer, new ArrayList<>()));
    }

    @Override
    public CompletableFuture<Integer> updateLao(String laoId, String name, long lastModified, List<String> witnesses) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new UpdateLao(name, lastModified, witnesses));
    }

    @Override
    public CompletableFuture<Integer> witnessMessage(String laoId, String messageId, String data) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new WitnessMessage(messageId, Signature.sign(privateKey, data)));
    }

    @Override
    public CompletableFuture<Integer> createMeeting(String laoId, String name, long creation, long lastModified, String location, long start, long end) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new CreateMeeting(Hash.hash("M", laoId, creation, name), name, creation, lastModified, location, start, end));
    }

    @Override
    public CompletableFuture<Integer> createRollCall(String laoId, String name, long creation, long start, CreateRollCall.StartType startType, String location) {
        return createRollCall(laoId, name, creation, start, startType, location, null);
    }

    @Override
    public CompletableFuture<Integer> createRollCall(String laoId, String name, long creation, long start, CreateRollCall.StartType startType, String location, @Nullable String description) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new CreateRollCall(Hash.hash("R", laoId, creation, name), name, creation, start, startType, location, description));
    }

    @Override
    public CompletableFuture<Integer> openRollCall(String laoId, String rollCallId, long start) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new OpenRollCall(rollCallId, start));
    }

    @Override
    public CompletableFuture<Integer> reopenRollCall(String laoId, String rollCallId, long start) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new ReopenRollCall(rollCallId, start));
    }

    @Override
    public CompletableFuture<Integer> closeRollCall(String laoId, String rollCallId, long start, long end, List<String> attendees) {
        return lowLevelClientProxy.publish(publicKey, privateKey, ROOT + "/" + laoId,
                new CloseRollCall(rollCallId, start, end, attendees));
    }

    @Override
    public void close(Throwable reason) {
        lowLevelClientProxy.close(reason);
    }

    @Override
    public void close() {
        close(new IOException("Socket closed"));
    }
}
