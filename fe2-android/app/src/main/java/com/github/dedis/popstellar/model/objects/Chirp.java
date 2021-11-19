package com.github.dedis.popstellar.model.objects;

/** Class modeling a Chirp */
public class Chirp {

  private String id;
  private String channel;

  private String sender;
  private String text;
  private Long timestamp;
  private Integer likes;
  private String parentId;

  public Chirp(String id) {
    if (id == null) {
      throw new IllegalArgumentException(" The id is null");
    } else if (id.isEmpty()) {
      throw new IllegalArgumentException(" The id of the Chirp is empty");
    }
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public Integer getLikes() {
    return likes;
  }

  public void setLikes(Integer likes) {
    this.likes = likes;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @Override
  public String toString() {
    return String.format(
        "Chirp{id='%s', channel='%s', sender='%s', text='%s', timestamp='%s', likes='%s', parentId='%s'",
        id, channel, sender, text, timestamp, likes, parentId);
  }
}
