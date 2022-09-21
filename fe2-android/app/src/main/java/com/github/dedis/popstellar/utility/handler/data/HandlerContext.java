package com.github.dedis.popstellar.utility.handler.data;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.remote.MessageSender;

public final class HandlerContext {

  private final MessageID messageId;
  private final PublicKey senderPk;
  private final MessageSender messageSender;
  private final Channel channel;

  public HandlerContext(
      @NonNull MessageID messageId,
      @NonNull PublicKey senderPk,
      @NonNull Channel channel,
      @NonNull MessageSender messageSender) {
    this.messageId = messageId;
    this.senderPk = senderPk;
    this.messageSender = messageSender;
    this.channel = channel;
  }

  public MessageSender getMessageSender() {
    return messageSender;
  }

  public Channel getChannel() {
    return channel;
  }

  public MessageID getMessageId() {
    return messageId;
  }

  public PublicKey getSenderPk() {
    return senderPk;
  }
}
