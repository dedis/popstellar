package ch.epfl.pop

import java.util.Arrays

import ch.epfl.pop.crypto.Hash
import ch.epfl.pop.json.{MessageContent, MessageErrorContent}
import ch.epfl.pop.crypto.Signature.verify
import ch.epfl.pop.json.JsonUtils.ErrorCodes.InvalidData

object Validate {

  /**
   * Verify that the signature and the id of the message are correct
   * @param content the message
   * @return None if correct, an error otherwise
   */
  def validate(content: MessageContent): Option[MessageErrorContent] = {
    if(!verify(content.encodedData, content.signature, content.sender)) {
      Some(MessageErrorContent(InvalidData.id, "Invalid signature"))
    }
    else {
      val id = Hash.computeID(content.encodedData, content.signature)
      if(Arrays.equals(id, content.message_id)) {
        None
      }
      else {
        Some(MessageErrorContent(InvalidData.id, "Invalid message id"))
      }
    }
  }

}
