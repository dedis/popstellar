package com.github.dedis.popstellar.repository.remote;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.method.Message;
import com.tinder.scarlet.WebSocket;
import com.tinder.scarlet.ws.Receive;
import com.tinder.scarlet.ws.Send;

import io.reactivex.Observable;

public interface LAOService {

  @Send
  void sendMessage(Message msg);

  @Receive
  Observable<GenericMessage> observeMessage();

  @Receive
  Observable<WebSocket.Event> observeWebsocket();
}
