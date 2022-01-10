package com.github.dedis.popstellar.utility.handler.data;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;

public final class HandlerContext {

  private final LAORepository laoRepository;
  private final String channel;
  private final MessageGeneral message;

  public HandlerContext(
      @NonNull LAORepository laoRepository,
      @NonNull String channel,
      @NonNull MessageGeneral message) {
    this.laoRepository = laoRepository;
    this.channel = channel;
    this.message = message;
  }

  public LAORepository getLaoRepository() {
    return laoRepository;
  }

  public String getChannel() {
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
}
