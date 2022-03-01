package ch.epfl.pop.pubsub.graph.validators

trait EventValidator {
  // String used as event distinction during the hashing process
  // Example: SHA256("M" || laoId || creation || name)
  //    --> "M" is the hash prefix in this situation
  val EVENT_HASH_PREFIX: String
}
