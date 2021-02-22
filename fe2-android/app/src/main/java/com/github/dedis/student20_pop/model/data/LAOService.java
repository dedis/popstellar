package com.github.dedis.student20_pop.model.data;

import com.github.dedis.student20_pop.model.network.GenericMessage;
import com.github.dedis.student20_pop.model.network.method.Message;
import com.tinder.scarlet.ws.Receive;
import com.tinder.scarlet.ws.Send;
import io.reactivex.Flowable;

public interface LAOService {

  @Send
  void sendMessage(Message msg);

  @Receive
  Flowable<GenericMessage> observeMessage();
}
