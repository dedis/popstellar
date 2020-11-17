package com.github.dedis.student20_pop.utility.network;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.github.dedis.student20_pop.model.network.level.high.lao.CreateLao;
import com.github.dedis.student20_pop.model.network.level.high.lao.UpdateLao;
import com.github.dedis.student20_pop.model.network.level.high.meeting.CreateMeeting;
import com.github.dedis.student20_pop.model.network.level.high.message.WitnessMessage;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;

import java.util.ArrayList;
import java.util.List;

import javax.websocket.Session;

@RequiresApi(api = Build.VERSION_CODES.N)
public final class HighLevelClientProxy {

    private final LowLevelClientProxy lowLevelClientProxy;

    public HighLevelClientProxy(Session session) {
        lowLevelClientProxy = new LowLevelClientProxy(session);
    }

    public LowLevelClientProxy lowLevel() {
        return lowLevelClientProxy;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createLoa(String name, long creation, long lastModified, String organizer) {
        lowLevelClientProxy.publish(me(), "/root",
                new CreateLao(Hash.hash(organizer + creation + name), name, creation, lastModified, organizer, new ArrayList<>()));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void updateLao(String laoId, String name, long lastModified, List<String> witnesses) {
        lowLevelClientProxy.publish(me(), "/root/" + laoId,
                new UpdateLao(name, lastModified, witnesses));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void witnessMessage(String laoId, String messageId, String data) {
        lowLevelClientProxy.publish(me(), "/root/" + laoId,
                new WitnessMessage(messageId, Signature.sign(key(), data)));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createMeeting(String laoId, String name, long creation, long lastModified, String location, long start, long end) {
        lowLevelClientProxy.publish(me(), "/root/" + laoId,
                new CreateMeeting(Hash.hash(laoId + creation + name), name, creation, lastModified, location, start, end));
    }

    private String me() {
        return null; //TODO
    }

    private String key() {
        return null; //TODO
    }
}
