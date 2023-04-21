package com.github.dedis.popstellar.utility.handler;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.repository.MessageRepository;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.handler.data.HandlerContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/** General message handler class */
@Singleton
public final class MessageHandler {

  private static final Logger logger = LogManager.getLogger(MessageHandler.class);

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
          UnknownElectionException, NoRollCallException {
    logger.debug("handle incoming message");

    if (messageRepo.isMessagePresent(message.getMessageId())) {
      logger.debug("the message has already been handled in the past");
      return;
    }

    Data data = message.getData();
    logger.debug("data with class: " + data.getClass());
    Objects dataObj = Objects.find(data.getObject());
    Action dataAction = Action.find(data.getAction());

    registry.handle(
        new HandlerContext(message.getMessageId(), message.getSender(), channel, messageSender),
        data,
        dataObj,
        dataAction);

    // Put the message in the state
    messageRepo.addMessage(message);
  }
}
