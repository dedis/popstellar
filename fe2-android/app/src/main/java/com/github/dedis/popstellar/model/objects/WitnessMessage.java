package com.github.dedis.popstellar.model.objects;

import java.util.HashSet;
import java.util.Set;

/**
 * Class to model a message that needs to be signed by witnesses
 */
public class WitnessMessage {

  private String messageId;
  /**
   * Base 64 URL encoded ID of the message that we want to sign
   */
  private Set<String> witnesses;
  /**
   * Set of witnesses that have signed the message
   */
  private String title = "";
  /**
   * Title that will be displayed for the message
   */
  private String description = ""; /** Description that will be displayed for the message*/

  /**
   * Constructor for a  Witness Message
   *
   * @param messageId ID of the message to sign
   */
  public WitnessMessage(String messageId) {
    witnesses = new HashSet<>();
    this.messageId = messageId;
  }

  /**
   * Method to add a new witness that have signed the message
   *
   * @param pk public key of the witness that have signed  the message
   */
  public void addWitness(String pk) {
    witnesses.add(pk);
  }

  public String getMessageId() {
    return messageId;
  }

  public Set<String> getWitnesses() {
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
