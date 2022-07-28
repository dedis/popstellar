package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Class to model a message that needs to be signed by witnesses */
public class WitnessMessage {

  /** Base 64 URL encoded ID of the message that we want to sign */
  private final MessageID messageId;
  /** Set of witnesses that have signed the message */
  private final Set<PublicKey> witnesses;
  /** Title that will be displayed for the message */
  private String title = "";
  /** Description that will be displayed for the message */
  private String description = "";

  /**
   * Constructor for a Witness Message
   *
   * @param messageId ID of the message to sign
   */
  public WitnessMessage(MessageID messageId) {
    witnesses = new HashSet<>();
    this.messageId = messageId;
  }

  public WitnessMessage(WitnessMessage witnessMessage) {
    this.messageId = new MessageID(witnessMessage.messageId);
    this.witnesses =
        witnessMessage.witnesses.stream().map(PublicKey::new).collect(Collectors.toSet());
    this.title = witnessMessage.title;
    this.description = witnessMessage.description;
  }

  /**
   * Method to add a new witness that have signed the message
   *
   * @param pk public key of the witness that have signed the message
   */
  public void addWitness(PublicKey pk) {
    witnesses.add(pk);
  }

  public MessageID getMessageId() {
    return messageId;
  }

  public Set<PublicKey> getWitnesses() {
    return witnesses;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @NonNull
  @Override
  public String toString() {
    return "WitnessMessage{"
        + "messageId='"
        + messageId
        + '\''
        + ", witnesses="
        + witnesses
        + ", title='"
        + title
        + '\''
        + ", description='"
        + description
        + '\''
        + '}';
  }
}
