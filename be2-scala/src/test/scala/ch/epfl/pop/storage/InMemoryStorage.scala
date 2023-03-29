package ch.epfl.pop.storage

case class InMemoryStorage(initial: Map[String, String] = Map.empty) extends Storage {
  var elements: Map[String, String] = initial
  def size: Int = elements.size

  override def read(key: String): Option[String] = elements.get(key)

  // Note: this write does NOT write as batch
  override def write(keyValues: (String, String)*): Unit = {
    for (kv <- keyValues) {
      // println(s"*** Writing in InMemoryStorage : '${kv._1} -> ${kv._2}'")
      elements += (kv._1 -> kv._2)
    }
  }

  override def delete(key: String): Unit = elements -= key

  override def close(): Unit = ()

  def dump(): Unit = for ((k, v) <- elements) println(s"  > $k | $v")

  /**
   *
   * @return the set of all channel keys in the DB.
   */
  override def getAllKeys(): Set[String] = ???
}
