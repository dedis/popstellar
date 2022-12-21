package com.github.dedis.popstellar.utility.handler;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.repository.MessageRepository;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.handler.data.HandlerContext;

import javax.inject.Inject;
import javax.inject.Singleton;

/** General message handler class */
@Singleton
public final class MessageHandler {

  public static final String TAG = MessageHandler.class.getSimpleName();

  private final MessageRepository messageRepo;
  private final DataRegistry registry;

  @Inject
  public MessageHandler(MessageRepository messageRepo, DataRegistry registry) {
    this.messageRepo = messageRepo;
    this.registry = registry;
  }

  /**
   * Send messages to the corresponding handler.
   *
   * @param messageSender the service used to send messages to the backend
   * @param channel the channel on which the message was received
   * @param message the message that was received
   */
  public void handleMessage(MessageSender messageSender, Channel channel, MessageGeneral message)
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException {
    Log.d(TAG, "handle incoming message");
    // Put the message in the state
    messageRepo.addMessage(message);

    Data data = message.getData();
    Log.d(TAG, "data with class: " + data.getClass());
    Objects dataObj = Objects.find(data.getObject());
    Action dataAction = Action.find(data.getAction());

    registry.handle(
        new HandlerContext(message.getMessageId(), message.getSender(), channel, messageSender),
        data,
        dataObj,
        dataAction);
  }
}
