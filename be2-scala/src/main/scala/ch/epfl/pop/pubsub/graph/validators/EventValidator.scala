package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.objects.{Hash, Timestamp}

trait EventValidator {
  /**
   * Generates a hash to validate event ids
   *
   * @param hash 2nd hash parameter
   * @param timestamp 3rd hash parameter
   * @param string 4th hash parameter
   * @return the calculated id
   */
  def generateValidationId(hash: Hash, timestamp: Timestamp, string: String): Hash

  def EVENT_HASH_PREFIX: String
}
