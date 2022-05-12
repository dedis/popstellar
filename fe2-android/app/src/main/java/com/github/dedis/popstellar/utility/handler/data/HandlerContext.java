package com.github.dedis.popstellar.utility.handler.data;

import androidx.annotation.NonNull;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.ServerRepository;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.security.KeyManager;

public final class HandlerContext {

  private final LAORepository laoRepository;
  private final KeyManager keyManager;
  private final MessageSender messageSender;
  private final Channel channel;
  private final MessageGeneral message;
  private final ServerRepository serverRepository;

  public HandlerContext(
      @NonNull LAORepository laoRepository,
      @NonNull KeyManager keyManager,
      @NonNull MessageSender messageSender,
      @NonNull Channel channel,
      @NonNull MessageGeneral message,
      @NonNull ServerRepository serverRepository) {
    this.laoRepository = laoRepository;
    this.keyManager = keyManager;
    this.messageSender = messageSender;
    this.channel = channel;
    this.message = message;
    this.serverRepository = serverRepository;
  }

  public LAORepository getLaoRepository() {
    return laoRepository;
  }

  public KeyManager getKeyManager() {
    return keyManager;
  }

  public MessageSender getMessageSender() {
    return messageSender;
  }

  public Channel getChannel() {
    return channel;
  }

  public MessageGeneral getMessage() {
    return message;
  }

  public MessageID getMessageId() {
    return message.getMessageId();
  }

  public PublicKey getSenderPk() {
    return message.getSender();
  }

  public ServerRepository getServerRepository() {
    return serverRepository;
  }
}
