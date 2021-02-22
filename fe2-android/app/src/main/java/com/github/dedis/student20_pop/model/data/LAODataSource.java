package com.github.dedis.student20_pop.model.data;

import com.github.dedis.student20_pop.model.entities.LAO;
import com.github.dedis.student20_pop.model.entities.LAOEntity;
import com.github.dedis.student20_pop.model.entities.Meeting;
import com.github.dedis.student20_pop.model.entities.ModificationSignature;
import com.github.dedis.student20_pop.model.entities.Person;
import com.github.dedis.student20_pop.model.entities.RollCall;
import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.method.Message;
import io.reactivex.Flowable;
import java.util.List;

public interface LAODataSource {

  interface Remote {

    Flowable<GenericMessage> observeMessage();

    void sendMessage(Message msg);
  }

  interface Local {

    List<LAO> getAll();

    LAOEntity getLAO(String channel);

    void updateLAO(LAO lao, List<Person> witnesses, List<ModificationSignature> signatures);

    void addRollCall(LAO lao, RollCall rollCall);

    void updateRollCall(RollCall rollCall);

    void addMeeting(LAO lao, Meeting meeting);

    void updateMeeting(Meeting meeting, List<ModificationSignature> signatures);
  }
}
