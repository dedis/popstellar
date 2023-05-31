package ch.epfl.pop.storage

trait Storage {

  /* List of prefix to the keys */
  final val CHANNEL_DATA_KEY = "ChannelData:"
  final val DATA_KEY = "Data:"
  final val CREATE_LAO_KEY = "CreateLaoId:"
  final val SETUP_ELECTION_KEY = "SetupElectionMessageId:"
  final val SERVER_PUBLIC_KEY = "ServerPublicKey:"
  final val SERVER_PRIVATE_KEY = "ServerPrivateKey:"

  /** Optionally returns the value associated with a key
    *
    * @param key
    *   the key value
    * @return
    *   an option value containing the value associated with <key> in the storage, or [[None]] if none exists
    */
  def read(key: String): Option[String]

  /** Adds a single/multiple key-value pairs in the storage
    *
    * @param keyValues
    *   collection of pairs of key and its associated values
    */
  def write(keyValues: (String, String)*): Unit

  def filterKeysByPrefix(prefix: String): Set[String]

  /** Removes a single element from the storage
    *
    * @param key
    *   the key value
    */
  def delete(key: String): Unit

  /** Cleanup after use
    */
  def close(): Unit
}
