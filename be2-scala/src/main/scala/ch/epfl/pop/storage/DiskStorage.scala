package ch.epfl.pop.storage

import java.io.{File, IOException}
import java.nio.charset.StandardCharsets

import org.iq80.leveldb.impl.Iq80DBFactory.factory
import org.iq80.leveldb.{DB, Options, WriteBatch}
import org.slf4j.{Logger, LoggerFactory}

class DiskStorage(val databaseFolder: String = DiskStorage.DATABASE_FOLDER) extends Storage {

  // create a logger for the database system
  private val logger: Logger = LoggerFactory.getLogger("DbLogger")

  private val options: Options = new Options()
  options.createIfMissing(true)
  options.cacheSize(DiskStorage.CACHE_SIZE)

  private val db: DB = {
    try factory.open(new File(databaseFolder), options)
    catch {
      case e: IOException =>
        logger.error("Could not open database folder '{}': {}", databaseFolder, e.getMessage)
        throw e
    }
  }

  def close(): Unit = db.close()


  @throws [org.iq80.leveldb.DBException]
  def read(key: String): Option[String] = {
    db.get(key.getBytes(StandardCharsets.UTF_8)) match {
      case null => None
      case bytes => Some(new String(bytes, StandardCharsets.UTF_8))
    }
  }

  @throws [org.iq80.leveldb.DBException]
  def write(key: String, value: String): Unit = {
    db.put(key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8))
  }

  @throws [org.iq80.leveldb.DBException]
  def write(keyValues: (String, String)*): Unit = keyValues.size match {
    // return early if nothing is being written
    case 0 =>

    // write a single (key -> value) pair in the db
    case 1 => write(keyValues.head._1, keyValues.head._2)

    // use a batch to write multiple (key -> value) pairs in the db
    case _ =>
      val batch: WriteBatch = db.createWriteBatch()

      try {
        for (kv <- keyValues) {
          batch.put(kv._1.getBytes(StandardCharsets.UTF_8), kv._2.getBytes(StandardCharsets.UTF_8))
        }

        db.write(batch)

      } finally {
        batch.close()
      }
  }

  @throws [org.iq80.leveldb.DBException]
  def delete(key: String): Unit = {
    db.delete(key.getBytes(StandardCharsets.UTF_8))
  }
}

object DiskStorage {
  val DATABASE_FOLDER: String = "database-new"

  private val CACHE_SIZE: Long = 64 * 1024 * 1024 // 64 MB
}
