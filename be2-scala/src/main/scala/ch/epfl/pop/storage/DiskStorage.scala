package ch.epfl.pop.storage

import java.io.{File, IOException}
import java.nio.charset.StandardCharsets

import org.iq80.leveldb.{DB, Options, WriteBatch}
import org.iq80.leveldb.impl.Iq80DBFactory.factory
import org.slf4j.{Logger, LoggerFactory}

class DiskStorage(val databaseFolder: String = DiskStorage.DATABASE_FOLDER) {

  // create a logger for the database system
  val logger: Logger = LoggerFactory.getLogger("DbLogger")

  val options: Options = new Options()
  options.createIfMissing(true)
  options.cacheSize(DiskStorage.CACHE_SIZE)

  val db: DB = {
    try factory.open(new File(databaseFolder), options)
    catch {
      case e: IOException =>
        logger.error("Could not open database folder '{}': {}", databaseFolder, e.getMessage)
        throw e
    }
  }

  def close(): Unit = db.close()


  @throws [org.iq80.leveldb.DBException]
  def read(key: String): String = {
    new String(db.get(key.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
  }

  @throws [org.iq80.leveldb.DBException]
  def write(keyValues: (String, String)*): Unit = {
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

  private val CACHE_SIZE: Long = 50 * 1024 * 1024 // 50 MB
}
