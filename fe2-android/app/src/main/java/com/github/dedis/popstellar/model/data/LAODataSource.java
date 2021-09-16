package com.github.dedis.popstellar.model.data;

import com.github.dedis.popstellar.model.entities.LAO;
import com.github.dedis.popstellar.model.entities.LAOEntity;
import com.github.dedis.popstellar.model.entities.Meeting;
import com.github.dedis.popstellar.model.entities.ModificationSignature;
import com.github.dedis.popstellar.model.entities.Person;
import com.github.dedis.popstellar.model.entities.RollCall;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.tinder.scarlet.WebSocket;
import io.reactivex.Observable;
import java.util.List;

public interface LAODataSource {

  interface Remote {

    Observable<GenericMessage> observeMessage();

    Observable<WebSocket.Event> observeWebsocket();

    void sendMessage(Message msg);

    int getRequestId();

    int incrementAndGetRequestId();
  }

  interface Local {

    List<LAO> getAll();

    LAOEntity getLAO(String channel);

    void addLao(LAO lao);

    void updateLAO(LAO lao, List<Person> witnesses, List<ModificationSignature> signatures);

    void addRollCall(LAO lao, RollCall rollCall);

    void updateRollCall(RollCall rollCall);

    void addMeeting(LAO lao, Meeting meeting);

    void updateMeeting(Meeting meeting, List<ModificationSignature> signatures);
  }
}
