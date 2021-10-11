package com.github.dedis.popstellar.repository;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.github.dedis.popstellar.repository.local.entities.LAOEntity;
import com.github.dedis.popstellar.repository.local.entities.LAOEntityRelation;
import com.github.dedis.popstellar.repository.local.entities.MeetingEntity;
import com.github.dedis.popstellar.repository.local.entities.ModificationSignatureEntity;
import com.github.dedis.popstellar.repository.local.entities.PersonEntity;
import com.github.dedis.popstellar.repository.local.entities.RollCallEntity;
import com.tinder.scarlet.WebSocket;

import java.util.List;

import io.reactivex.Observable;

public interface LAODataSource {

  interface Remote {

    Observable<GenericMessage> observeMessage();

    Observable<WebSocket.Event> observeWebsocket();

    void sendMessage(Message msg);

    int getRequestId();

    int incrementAndGetRequestId();
  }

  interface Local {

    List<LAOEntity> getAll();

    LAOEntityRelation getLAO(String channel);

    void addLao(LAOEntity lao);

    void updateLAO(
        LAOEntity lao, List<PersonEntity> witnesses, List<ModificationSignatureEntity> signatures);

    void addRollCall(LAOEntity lao, RollCallEntity rollCall);

    void updateRollCall(RollCallEntity rollCall);

    void addMeeting(LAOEntity lao, MeetingEntity meeting);

    void updateMeeting(MeetingEntity meeting, List<ModificationSignatureEntity> signatures);
  }
}
