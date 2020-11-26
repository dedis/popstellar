package com.github.dedis.student20_pop.utility.network;

import com.github.dedis.student20_pop.model.network.level.high.lao.CreateLao;
import com.github.dedis.student20_pop.model.network.level.high.lao.UpdateLao;
import com.github.dedis.student20_pop.model.network.level.high.meeting.CreateMeeting;
import com.github.dedis.student20_pop.model.network.level.high.message.WitnessMessage;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;

import java.util.ArrayList;
import java.util.List;

import javax.websocket.Session;

public final class HighLevelClientProxy {

    private final LowLevelClientProxy lowLevelClientProxy;
    private final String publicKey, privateKey;

    public HighLevelClientProxy(Session session, String publicKey, String privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        lowLevelClientProxy = new LowLevelClientProxy(session);
    }

    public LowLevelClientProxy lowLevel() {
        return lowLevelClientProxy;
    }

    public void createLoa(String name, long creation, long lastModified, String organizer) {
        lowLevelClientProxy.publish(publicKey, "/root",
                new CreateLao(Hash.hash(organizer + creation + name), name, creation, lastModified, organizer, new ArrayList<>()));
    }

    public void updateLao(String laoId, String name, long lastModified, List<String> witnesses) {
        lowLevelClientProxy.publish(publicKey, "/root/" + laoId,
                new UpdateLao(name, lastModified, witnesses));
    }

    public void witnessMessage(String laoId, String messageId, String data) {
        lowLevelClientProxy.publish(publicKey, "/root/" + laoId,
                new WitnessMessage(messageId, Signature.sign(privateKey, data)));
    }

    public void createMeeting(String laoId, String name, long creation, long lastModified, String location, long start, long end) {
        lowLevelClientProxy.publish(publicKey, "/root/" + laoId,
                new CreateMeeting(Hash.hash(laoId + creation + name), name, creation, lastModified, location, start, end));
    }
}
