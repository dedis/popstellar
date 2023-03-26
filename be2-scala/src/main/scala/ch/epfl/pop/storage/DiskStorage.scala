package ch.epfl.pop.storage

import java.io.{File, IOException}
import java.nio.charset.StandardCharsets
import ch.epfl.pop.model.objects.DbActorNAckException
import ch.epfl.pop.pubsub.graph.ErrorCodes
import org.iq80.leveldb.impl.Iq80DBFactory.{asString, factory}
import org.iq80.leveldb.{DB, Options, WriteBatch}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

class DiskStorage(val databaseFolder: String = DiskStorage.DATABASE_FOLDER) extends Storage { // ajouter une liste de channels

  // create a logger for the database system
  private val logger: Logger = LoggerFactory.getLogger("DbLogger")
  private final val CHANNELS_KEY: String = "channels_key"

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

  @throws[DbActorNAckException]
  def read(key: String): Option[String] = {
    Try(db.get(key.getBytes(StandardCharsets.UTF_8))) match {
      case Success(null)  => None // if the key does not exist in DiskStorage
      case Success(bytes) => Some(new String(bytes, StandardCharsets.UTF_8))
      case Failure(ex) => throw DbActorNAckException(
          ErrorCodes.SERVER_ERROR.id,
          s"could not read key '$key' from DiskStorage : ${ex.getMessage}"
        )
    }
  }

  @throws[DbActorNAckException]
  def write(keyValues: (String, String)*): Unit = {
    // use a batch to write multiple (key -> value) pairs in the db
    val batch: WriteBatch = db.createWriteBatch()

    try {
      for (kv <- keyValues) {
        batch.put(kv._1.getBytes(StandardCharsets.UTF_8), kv._2.getBytes(StandardCharsets.UTF_8))
      }

      db.write(batch)

    } catch {
      case ex: Throwable => throw DbActorNAckException(
          ErrorCodes.SERVER_ERROR.id,
          s"could not write ${keyValues.size} elements to DiskStorage : ${ex.getMessage}"
        )
    } finally {
      batch.close()
    }
  }

  def getAllKeys(): Set[String] = {

    var set: Set[String] = Set()
    val iterator = db.iterator()
    iterator.seekToFirst()

    while (iterator.hasNext) {
      val str = asString(iterator.next().getKey)
      if (str.length == 50)
        set += str
    }
    set
  }

  @throws[DbActorNAckException]
  def delete(key: String): Unit = {
    // delete returns Unit if:
    //  - the key was present in the DiskStorage and deleted, or
    //  - the key was not in the DiskStorage
    // An exception is thrown only in the case of a database error
    Try(db.delete(key.getBytes(StandardCharsets.UTF_8))).recover(ex =>
      throw DbActorNAckException(
        ErrorCodes.SERVER_ERROR.id,
        s"could not delete key '$key' from DiskStorage : ${ex.getMessage}"
      )
    )
  }

}

object DiskStorage {
  val DATABASE_FOLDER: String = "database"

  private val CACHE_SIZE: Long = 64 * 1024 * 1024 // 64 MB
}
