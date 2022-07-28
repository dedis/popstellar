package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

/** Class modeling a Chirp */
public class Chirp {

  private static final int MAX_CHIRP_CHARS = 300;

  private MessageID id;
  private Channel channel;

  private PublicKey sender;
  private String text;
  private long timestamp;
  private boolean isDeleted;
  private MessageID parentId;

  public Chirp(MessageID id) {
    if (id == null) {
      throw new IllegalArgumentException("The id is null");
    } else if (id.getEncoded().isEmpty()) {
      throw new IllegalArgumentException("The id of the Chirp is empty");
    }
    this.id = id;
  }

  public Chirp(Chirp chirp) {
    this.id = new MessageID(chirp.id);
    this.channel = new Channel(chirp.channel);
    this.sender = new PublicKey(chirp.sender);
    this.text = chirp.text;
    this.timestamp = chirp.timestamp;
    this.isDeleted = chirp.isDeleted;
    this.parentId = new MessageID(chirp.parentId);
  }

  public MessageID getId() {
    return id;
  }

  public void setId(MessageID id) {
    if (id == null) {
      throw new IllegalArgumentException("The id is null");
    } else if (id.getEncoded().isEmpty()) {
      throw new IllegalArgumentException("The id of the Chirp is empty");
    }
    this.id = id;
  }

  public Channel getChannel() {
    return channel;
  }

  public void setChannel(@NonNull Channel channel) {
    this.channel = channel;
  }

  public PublicKey getSender() {
    return sender;
  }

  public void setSender(PublicKey sender) {
    this.sender = sender;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    if (text.length() > MAX_CHIRP_CHARS) {
      throw new IllegalArgumentException("the text exceed the maximum numbers of characters");
    }
    this.text = text;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public boolean getIsDeleted() {
    return isDeleted;
  }

  public void setIsDeleted(boolean isDeleted) {
    this.isDeleted = isDeleted;
  }

  public MessageID getParentId() {
    return parentId;
  }

  public void setParentId(MessageID parentId) {
    this.parentId = parentId;
  }

  @NonNull
  @Override
  public String toString() {
    return String.format(
        "Chirp{id='%s', channel='%s', sender='%s', text='%s', timestamp='%s', isDeleted='%s', parentId='%s'",
        id.getEncoded(), channel, sender, text, timestamp, isDeleted, parentId.getEncoded());
  }
}
