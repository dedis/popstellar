package ch.epfl.pop.storage

trait Storage {

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

  /** Removes a single element from the storage
    *
    * @param key
    *   the key value
    */
  def delete(key: String): Unit

  /** Cleanup after use
    */
  def close(): Unit

  /**
   *
   * @return the set of all channel keys in the DB.
   */
  def getAllKeys(): Set[String]
}
